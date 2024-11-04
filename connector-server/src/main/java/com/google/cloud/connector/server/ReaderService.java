package com.google.cloud.connector.server;

import static com.google.cloud.connector.data.ResultStreamId.COLLECTION_ID_STREAM;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_READ_STREAM;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_STATUS_FAILED;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_STATUS_SUCCESS;

import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.bigquery.federation.v1alpha1.ReadStreamRequest;
import com.google.cloud.bigquery.federation.v1alpha1.ReaderServiceGrpc;
import com.google.cloud.bigquery.federation.v1alpha1.ReaderServiceGrpc.ReaderServiceImplBase;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.DatasetName;
import com.google.cloud.connector.api.NativeQuerySchemaResolver;
import com.google.cloud.connector.api.ParallelQueryExecutor;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.exception.ConnectorException;
import com.google.cloud.connector.data.AssetInfo;
import com.google.cloud.connector.data.Partition;
import com.google.cloud.connector.data.ResultStreamId;
import com.google.cloud.connector.server.common.BatchingRecordStreamResponder;
import com.google.cloud.connector.server.metrics.ConnectorMetricsUtil;
import com.google.cloud.connector.server.metrics.DataBatchMetricsRecorder;
import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.OpenTelemetry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Implementation of the gRPC {@link ReaderServiceGrpc}.
 */
class ReaderService extends ReaderServiceImplBase implements BatchingRecordStreamResponder {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ConnectorLoaderFactory connectorLoaderFactory;
  private final OpenTelemetry openTelemetry;
  private final Provider<DataSchemaBuilder> dataSchemaBuilderProvider;
  private static final Gson GSON = new Gson();

  @Inject
  ReaderService(
      ConnectorLoaderFactory connectorLoaderFactory,
      Provider<DataSchemaBuilder> dataSchemaBuilderProvider,
      OpenTelemetry openTelemetry) {
    this.connectorLoaderFactory = connectorLoaderFactory;
    this.dataSchemaBuilderProvider = dataSchemaBuilderProvider;
    this.openTelemetry = openTelemetry;
  }

  @Override
  public void readStream(ReadStreamRequest request, StreamObserver<Data> responseObserver) {
    Instant start = Instant.now();
    Deadline deadline = Context.current().getDeadline();
    logger.atInfo().log("Received ReadStream with deadline %s for stream : {%s}",
        deadline, request.getResultStream());

    DatasetName datasetName = DatasetName.fromName(request.getResultStream());

    String dataSourceId = datasetName.datasource();
    String partitionId = datasetName.getComponent(COLLECTION_ID_STREAM).resourceId();

    String status = API_STATUS_FAILED;

    try (ConnectorLoader loader = connectorLoaderFactory.get(dataSourceId)) {
      ParallelQueryExecutor connector =
          loader.instantiateConnector(request.getParameters(), ParallelQueryExecutor.class);

      DataBatchMetricsRecorder metricsRecorder =
          new DataBatchMetricsRecorder(openTelemetry, dataSourceId, API_READ_STREAM, start);

      byte[] partitionData;
      Partition partition = null;
      byte[] query;
      try {
        partitionData = ResultStreamId.decode(partitionId);
        partition = GSON.fromJson(
            new String(partitionData, StandardCharsets.UTF_8), Partition.class);
        query = partition.query();
      } catch (IllegalArgumentException | JsonSyntaxException ex) {
        // TODO(harshpruthi): Remove support for old partition type after it goes obsolete.
        // Keeping this logic for now to keep the change backward compatible.
        LOG.atInfo().log("Received partition is not a valid 'Partition' object."
            + "Treating the partition to be of older format.");
        query = ResultStreamId.decode((partitionId));
        //throw ConnectorException.newBuilder(String.format("Read Stream failed, "
        //      + "invalid partition id provided:'%s'", request.getResultStream()), ex)
        //    .setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT)
        //    .build();
      }

      if (partition != null && partition.isEmpty()) {
        if (partition.assetInfo() == null) {
          throw new ConnectorException.Builder(
              "Read Stream Failed: Neither query nor asset info was specified.", null)
              .setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT).build();
        }
        DataSchemaBuilder schemaBuilder = dataSchemaBuilderProvider.get();
        resolveSchemaInternal(partition.assetInfo(), loader,
            request, schemaBuilder);
        sendNoRecordBatch(responseObserver, schemaBuilder.getSchema(), metricsRecorder);
      } else {
        try (RecordReader reader =
            connector.readPartition(AssetName.ROOT_ASSET, query)) {
          sendRecordsInBatches(
              responseObserver,
              reader,
              dataSchemaBuilderProvider.get(),
              metricsRecorder);
        } catch (IOException e) {
          throw ConnectorException.newBuilder(
                  String.format("Read Stream Failed: %s", e.getMessage()), e)
              .setFailureReason(ConnectorException.FailureReason.INTERNAL)
              .build();
        }
      }
      status = API_STATUS_SUCCESS;
      logger.atInfo().log("Successfully processed ReadPartition request");
    } catch (IllegalArgumentException | ValidationException ex) {
      ConnectorException.Builder builder =
          new ConnectorException.Builder("Read Stream Failed: " + ex.getMessage(), ex);
      builder.setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT);
      throw builder.build();
    } finally {
      ConnectorMetricsUtil.recordApiCount(openTelemetry, dataSourceId, API_READ_STREAM, status);
    }
  }

  private void resolveSchemaInternal(
      AssetInfo assetInfo, ConnectorLoader loader,
      ReadStreamRequest request, DataSchemaBuilder schemaBuilder) {
    switch (assetInfo.getSourceCase()) {
      case NAMED_TABLE -> {
        var connector = loader.instantiateConnector(request.getParameters(), Connector.class);
        connector.resolveSchema(assetInfo.assetName(), schemaBuilder);
      }
      case NATIVE_QUERY -> {
        var connector =
            loader.instantiateConnector(request.getParameters(), NativeQuerySchemaResolver.class);
        connector.resolveSchema(assetInfo.assetName(), assetInfo.nativeQuery(), schemaBuilder);
      }
      default -> throw new IllegalArgumentException(
          String.format("Invalid ReadStream Id. Unsupported source case: %s",
              assetInfo.getSourceCase()));
    }
  }
}
