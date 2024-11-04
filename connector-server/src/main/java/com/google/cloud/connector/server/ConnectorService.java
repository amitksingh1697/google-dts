package com.google.cloud.connector.server;

import static com.google.cloud.connector.server.metrics.MetricsConstants.API_EXECUTE_QUERY;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_RESOLVE_SCHEMA;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_START_QUERY;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_STATUS_FAILED;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_STATUS_SUCCESS;

import com.google.cloud.bigquery.federation.v1alpha1.ConnectorServiceGrpc;
import com.google.cloud.bigquery.federation.v1alpha1.ConnectorServiceGrpc.ConnectorServiceImplBase;
import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.bigquery.federation.v1alpha1.DataSource;
import com.google.cloud.bigquery.federation.v1alpha1.ExecuteQueryRequest;
import com.google.cloud.bigquery.federation.v1alpha1.GetDataSourceRequest;
import com.google.cloud.bigquery.federation.v1alpha1.ResolveSchemaRequest;
import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryRequest;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.DatasetName;
import com.google.cloud.connector.api.NativeQuerySchemaResolver;
import com.google.cloud.connector.api.ParallelQueryExecutor;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.exception.ConnectorException;
import com.google.cloud.connector.data.AssetInfo;
import com.google.cloud.connector.data.ConnectorParallelQueryPreparationContext;
import com.google.cloud.connector.server.common.BatchingRecordStreamResponder;
import com.google.cloud.connector.server.metrics.ConnectorMetricsUtil;
import com.google.cloud.connector.server.metrics.DataBatchMetricsRecorder;
import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.OpenTelemetry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of the {@link ConnectorServiceGrpc} gRPC service.
 */
class ConnectorService extends ConnectorServiceImplBase implements BatchingRecordStreamResponder {

  private final ConnectorLoaderFactory connectorLoaderFactory;
  private final Provider<DataSchemaBuilder> dataSchemaBuilderProvider;
  private final OpenTelemetry openTelemetry;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Inject
  ConnectorService(
      ConnectorLoaderFactory connectorLoaderFactory,
      Provider<DataSchemaBuilder> dataSchemaBuilderProvider,
      OpenTelemetry openTelemetry) {
    this.connectorLoaderFactory = connectorLoaderFactory;
    this.dataSchemaBuilderProvider = dataSchemaBuilderProvider;
    this.openTelemetry = openTelemetry;
  }

  @Override
  public void resolveSchema(ResolveSchemaRequest request, StreamObserver<Schema> responseObserver) {
    Instant start = Instant.now();
    logger.atInfo().log("Received ResolveSchema request for dataset:{%s}, %s",
        request.getDataset(), getRequestQueryString(request));

    DatasetName datasetName = DatasetName.fromName(request.getDataset());
    String dataSourceId = datasetName.datasource();

    String status = API_STATUS_FAILED;
    try (ConnectorLoader loader = connectorLoaderFactory.get(dataSourceId)) {
      Schema schema = resolveSchemaInternal(loader, request).toBuilder()
          .setDataSource(loader.getDataSource())
          .build();
      responseObserver.onNext(schema);
      responseObserver.onCompleted();
      status = API_STATUS_SUCCESS;
    } catch (IllegalArgumentException | ValidationException ex) {
      throw ConnectorException.newBuilder("Resolve Schema Failed: " + ex.getMessage(), ex)
          .setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT)
          .build();
    } finally {
      ConnectorMetricsUtil.recordApiCountAndLatency(
          openTelemetry,
          dataSourceId,
          API_RESOLVE_SCHEMA,
          status,
          Duration.between(start, Instant.now()));
    }
  }

  @Override
  public void startQuery(
      StartQueryRequest request, StreamObserver<StartQueryResponse> responseObserver) {

    Instant start = Instant.now();
    Deadline deadline = Context.current().getDeadline();
    logger.atInfo().log("Received StartQuery request with deadline %s for dataset:{%s}, %s",
        deadline, request.getDataset(), getRequestQueryString(request.getDataQuery()));

    DatasetName datasetName = DatasetName.fromName(request.getDataset());
    String dataSourceId = datasetName.datasource();

    String status = API_STATUS_FAILED;
    try (ConnectorLoader loader = connectorLoaderFactory.get(dataSourceId)) {
      ParallelQueryExecutor connector =
          loader.instantiateConnector(request.getParameters(), ParallelQueryExecutor.class);

      AssetName connectorAssetName = getAssetNameFromDataQuery(request.getDataQuery());
      DataQuery.SourceCase connectorAssetType = request.getDataQuery().getSourceCase();
      ConnectorParallelQueryPreparationContext context =
          new ConnectorParallelQueryPreparationContext(
              datasetName, new AssetInfo(
                  connectorAssetName, request.getDataQuery().getNativeQuery()));
      connector.prepareQuery(connectorAssetName, request.getDataQuery(), context);

      responseObserver.onNext(context.buildStartQueryResponse());
      status = API_STATUS_SUCCESS;
      responseObserver.onCompleted();
    } catch (IllegalArgumentException | ValidationException ex) {
      ConnectorException.Builder builder =
          new ConnectorException.Builder("StartQuery Failed: " + ex.getMessage(), ex);
      builder.setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT);
      throw builder.build();
    } finally {
      ConnectorMetricsUtil.recordApiCountAndLatency(
          openTelemetry,
          dataSourceId,
          API_START_QUERY,
          status,
          Duration.between(start, Instant.now()));
    }
  }

  @Override
  public void executeQuery(ExecuteQueryRequest request, StreamObserver<Data> responseObserver) {

    Instant start = Instant.now();
    Deadline deadline = Context.current().getDeadline();
    logger.atInfo().log("Received ExecuteQuery request with deadline %s for dataset:{%s}, %s",
        deadline, request.getDataset(), getRequestQueryString(request.getDataQuery()));

    DatasetName datasetName = DatasetName.fromName(request.getDataset());
    String dataSourceId = datasetName.datasource();

    String status = API_STATUS_FAILED;
    try (ConnectorLoader loader = connectorLoaderFactory.get(dataSourceId)) {
      SynchronousQueryExecutor connector =
          loader.instantiateConnector(request.getParameters(), SynchronousQueryExecutor.class);

      AssetName connectorAsset = getAssetNameFromDataQuery(request.getDataQuery());
      try (RecordReader reader = connector.execute(connectorAsset, request.getDataQuery())) {
        sendRecordsInBatches(
            responseObserver,
            reader,
            dataSchemaBuilderProvider.get(),
            new DataBatchMetricsRecorder(openTelemetry, dataSourceId, API_EXECUTE_QUERY, start));
        status = API_STATUS_SUCCESS;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    } catch (IllegalArgumentException | ValidationException ex) {
      throw ConnectorException.newBuilder("Execute Failed: " + ex.getMessage(), ex)
          .setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT)
          .build();
    } finally {
      ConnectorMetricsUtil.recordApiCount(openTelemetry, dataSourceId, API_EXECUTE_QUERY, status);
    }
  }

  @Override
  public void getDataSource(
      GetDataSourceRequest request, StreamObserver<DataSource> responseObserver) {

    Deadline deadline = Context.current().getDeadline();
    logger.atInfo().log("Received GetDataSource request with deadline %s:{%s}",
        deadline, request);

    DatasetName datasetName = DatasetName.fromName(request.getName());
    String dataSourceId = datasetName.datasource();

    try (ConnectorLoader loader = connectorLoaderFactory.get(dataSourceId)) {
      responseObserver.onNext(loader.getDataSource());
      responseObserver.onCompleted();
    } catch (IllegalArgumentException | ValidationException ex) {
      throw ConnectorException.newBuilder(
              String.format("GetDataSource Failed '%s': %s", dataSourceId, ex.getMessage()), ex)
          .setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT)
          .build();
    }
  }

  private Schema resolveSchemaInternal(ConnectorLoader loader, ResolveSchemaRequest request) {
    DataSchemaBuilder schemaBuilder = dataSchemaBuilderProvider.get();
    switch (request.getSourceCase()) {
      case NAMED_TABLE -> {
        var connector = loader.instantiateConnector(request.getParameters(), Connector.class);
        connector.resolveSchema(AssetName.fromNamedTable(request.getNamedTable()), schemaBuilder);
      }
      case NATIVE_QUERY -> {
        var connector =
            loader.instantiateConnector(request.getParameters(), NativeQuerySchemaResolver.class);
        connector.resolveSchema(AssetName.ROOT_ASSET, request.getNativeQuery(), schemaBuilder);
      }
      default -> throw new IllegalArgumentException(
          String.format("Unsupported source case: %s", request.getSourceCase()));
    }
    return schemaBuilder.getSchema();
  }

  private AssetName getAssetNameFromDataQuery(DataQuery dataQuery) {
    return switch (dataQuery.getSourceCase()) {
      case NAMED_TABLE -> AssetName.fromNamedTable(dataQuery.getNamedTable());
      case NATIVE_QUERY -> AssetName.ROOT_ASSET;
      default -> throw new IllegalArgumentException(
          String.format("Unsupported source case: %s", dataQuery.getSourceCase()));
    };
  }

  private String getRequestQueryString(ResolveSchemaRequest request) {
    return switch (request.getSourceCase()) {
      case TABLE -> "table: {" + request.getTable() + "}";
      case NAMED_TABLE -> "named_table: {" + request.getNamedTable() + "}";
      case NATIVE_QUERY -> "native_query: {" + request.getNativeQuery() + "}";
      default -> null;
    };
  }

  private String getRequestQueryString(DataQuery dataQuery) {
    Preconditions.checkArgument(dataQuery != null,
        "DataQuery should not be null");
    return switch (dataQuery.getSourceCase()) {
      case TABLE -> "table: {" + dataQuery.getTable() + "}";
      case NAMED_TABLE -> "named_table: {" + dataQuery.getNamedTable() + "}";
      case NATIVE_QUERY -> "native_query: {" + dataQuery.getNativeQuery() + "}";
      default -> null;
    };
  }
}
