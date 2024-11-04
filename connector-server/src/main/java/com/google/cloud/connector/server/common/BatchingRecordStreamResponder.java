package com.google.cloud.connector.server.common;

import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.bigquery.federation.v1alpha1.Data.DataBlock;
import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.StructField;
import com.google.cloud.bigquery.federation.v1alpha1.StructType;
import com.google.cloud.bigquery.federation.v1alpha1.StructValue;
import com.google.cloud.bigquery.federation.v1alpha1.Type;
import com.google.cloud.bigquery.federation.v1alpha1.TypeKind;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.data.ConnectorRecordBuilder;
import com.google.cloud.connector.server.DataSchemaBuilder;
import com.google.cloud.connector.server.metrics.DataBatchMetricsRecorder;
import com.google.common.flogger.FluentLogger;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Interface which provides a default implementation to send records from a {@link RecordReader}
 * through a {@link StreamObserver} for {@link Data} entries. Implementations can use this default
 * method to batch and send requests.
 */
public interface BatchingRecordStreamResponder {
  long MAX_BATCH_SIZE_BYTES = 256_000; // 256kb
  ZoneOffset DATE_TIME_ZONE_OFFSET = ZoneOffset.UTC;
  FluentLogger LOG = FluentLogger.forEnclosingClass();

  /**
   * In case of an empty partition (no query), set the schema, send it and close the
   * {@link StreamObserver}.
   *
   * @param dataStreamObserver RPC stream observer used to send response
   * @param schema {@link Schema} of the asset
   * @param metricsRecorder of type {@link DataBatchMetricsRecorder} to record metrics
   */
  default void sendNoRecordBatch(
      StreamObserver<Data> dataStreamObserver,
      Schema schema,
      DataBatchMetricsRecorder metricsRecorder
  ) {
    Data.Builder dataBuilder = Data.newBuilder();
    dataBuilder.setSchema(schema);
    dataStreamObserver.onNext(dataBuilder.build());
    metricsRecorder.recordLatency();
    LOG.atInfo().log("Processed empty partition successfully.");
    dataStreamObserver.onCompleted();
  }

  /**
   * Send all records generated by the supplied {@link ConnectorRecordBuilder} using the supplied
   * {@link StreamObserver}. After all records are sent, the {@link StreamObserver} is closed.
   *
   * @param dataStreamObserver RPC stream observer used to send response
   * @param recordReader Record reader to use to generate records.
   * @param schemaBuilder a {@link DataSchemaBuilder} for creating {@link Schema}.
   * @throws IOException if the record reader fails when reading records.
   */
  default void sendRecordsInBatches(
      StreamObserver<Data> dataStreamObserver,
      RecordReader recordReader,
      DataSchemaBuilder schemaBuilder,
      DataBatchMetricsRecorder metricsRecorder)
      throws IOException {
    // Get the schemas for the inbound records and the response.
    recordReader.getRecordSchema(schemaBuilder);
    Schema dataSchema = schemaBuilder.getSchema();
    Type recordSchema = getStructType(dataSchema);

    // Send requests in batches, until there are no more records to send.
    // The first Data page would contain the schema.
    Data.Builder dataBuilder = Data.newBuilder();
    dataBuilder.setSchema(dataSchema);

    LOG.atInfo().log("Sending data in batches");
    while (sendBatch(dataStreamObserver, dataBuilder, recordReader, recordSchema, metricsRecorder)
        > 0) {
      dataBuilder = Data.newBuilder();
    }
    LOG.atInfo().log("Sending data complete");
    // Terminate the response
    dataStreamObserver.onCompleted();
  }

  /**
   * Send a batch of up to {@link BatchingRecordStreamResponder#MAX_BATCH_SIZE_BYTES} records though
   * the rpc streaming channel. Returns the number of records sent in this batch.
   *
   * @param dataStreamObserver RPC stream observer for {@link Data} pages.
   * @param dataBuilder a {@link Data.Builder} for building a {@link Data} page.
   * @param recordReader record reader to use to read records.
   * @param recordSchema schema to use for the Record Builder.
   * @return integer specifying how many records were sent in the current batch.
   * @throws IOException if the record reader fails when reading records.
   */
  private int sendBatch(
      StreamObserver<Data> dataStreamObserver,
      Data.Builder dataBuilder,
      RecordReader recordReader,
      Type recordSchema,
      DataBatchMetricsRecorder metricsRecorder)
      throws IOException {
    long batchSizeBytes = 0L;

    // Initialize Row Set builder for the Data page.
    Data.RowSet.Builder rowSetBuilder = Data.RowSet.newBuilder();

    // Initialize row builder
    ConnectorRecordBuilder rowBuilder;
    Instant start;
    start = Instant.now();
    try {
      while (batchSizeBytes < MAX_BATCH_SIZE_BYTES
          && recordReader.nextRecord(
          rowBuilder = new ConnectorRecordBuilder(recordSchema, DATE_TIME_ZONE_OFFSET))) {
        // Add row to row set builder, and add the size of the current record to the total page size
        StructValue currentRow = rowBuilder.getValue().getStructValue();
        rowSetBuilder.addRows(currentRow);
        batchSizeBytes += currentRow.getSerializedSize();
      }
    } catch (Exception ex) {
      LOG.atSevere().log("Error encountered after reading '%s' rows",
          rowSetBuilder.getRowsCount());
      throw ex;
    }

    int rowsCount = rowSetBuilder.getRowsCount();

    // Set the row set if there is data
    if (rowsCount > 0) {
      dataBuilder.setDataBlock(DataBlock.newBuilder().setRowset(rowSetBuilder));
    }

    // Send a data page through the response observer for two scenarios:
    // 1. Data is not empty
    // 2. If there is no data but the first batch, send a response with schema, so it is an
    // explicit empty data response.
    if (rowsCount > 0 || dataBuilder.hasSchema()) {
      dataStreamObserver.onNext(dataBuilder.build());
    }

    // record metrics
    if (dataBuilder.hasSchema()) {
      metricsRecorder.recordLatency();
    }
    metricsRecorder.recordMetrics(
        rowsCount, batchSizeBytes, Duration.between(start, Instant.now()));

    LOG.atInfo().log("Sent '%d' rows of '%d' bytes size in batch", rowsCount, batchSizeBytes);

    // return the number of records sent in this batch.
    return rowsCount;
  }

  /**
   * Get the {@link Type} representation based on a {@link Schema}.
   *
   * @param schema schema to use
   * @return {@link Type} representation for a {@link Schema}.
   */
  private Type getStructType(Schema schema) {
    StructType.Builder builder = StructType.newBuilder();
    for (Schema.Field field : schema.getFieldsList()) {
      builder.addFields(
          StructField.newBuilder()
              .setFieldName(field.getFieldName())
              .setFieldType(field.getType())
              .build());
    }
    return Type.newBuilder().setTypeKind(TypeKind.STRUCT).setStructType(builder).build();
  }
}
