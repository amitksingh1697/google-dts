package com.google.cloud.connector.server;

import static com.google.cloud.connector.server.metrics.MetricsConstants.API_BROWSE_ASSET;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_STATUS_FAILED;
import static com.google.cloud.connector.server.metrics.MetricsConstants.API_STATUS_SUCCESS;

import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.BrowseAsset;
import com.google.cloud.connector.api.BrowseRequest;
import com.google.cloud.connector.api.BrowseResponse;
import com.google.cloud.connector.api.DataExploreServiceGrpc;
import com.google.cloud.connector.api.DataExploreServiceGrpc.DataExploreServiceImplBase;
import com.google.cloud.connector.api.DataExplorer;
import com.google.cloud.connector.api.DatasetName;
import com.google.cloud.connector.api.browse.BrowseAssetResult;
import com.google.cloud.connector.api.exception.ConnectorException;
import com.google.cloud.connector.server.metrics.ConnectorMetricsUtil;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.OpenTelemetry;
import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of the {@link DataExploreServiceGrpc} gRPC service.
 */
public class DataExploreService extends DataExploreServiceImplBase {

  private final ConnectorLoaderFactory connectorLoaderFactory;
  private final OpenTelemetry openTelemetry;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Inject
  DataExploreService(ConnectorLoaderFactory connectorLoaderFactory, OpenTelemetry openTelemetry) {
    this.connectorLoaderFactory = connectorLoaderFactory;
    this.openTelemetry = openTelemetry;
  }

  @Override
  public void browse(BrowseRequest request, StreamObserver<BrowseResponse> responseObserver) {
    Instant start = Instant.now();
    Deadline deadline = Context.current().getDeadline();
    logger.atInfo().log("Received Browse request with deadline %s for BrowseAsset:{%s}",
        deadline, request.getAsset());

    BrowseAsset parentAsset = request.getAsset();
    DatasetName datasetName = DatasetName.fromName(parentAsset.getDataset());
    String dataSourceId = datasetName.datasource();
    String status = API_STATUS_FAILED;
    try (ConnectorLoader loader = connectorLoaderFactory.get(dataSourceId)) {
      DataExplorer dataExplorer =
          loader.instantiateConnector(request.getParameters(), DataExplorer.class);

      AssetName assetName = AssetName.fromNamedTable(parentAsset.getNamedTable());

      BrowseAssetResult assets = dataExplorer.browseAsset(assetName);

      BrowseResponse.Builder builder = BrowseResponse.newBuilder();
      // Prepend the datasources/{datasource-id} to the assets
      assets.forEach(asset -> builder.addAssets(
          BrowseAsset.newBuilder(asset).setDataset(datasetName.name()).build()));

      responseObserver.onNext(builder.setParentAsset(parentAsset).build());
      responseObserver.onCompleted();
      status = API_STATUS_SUCCESS;
    } catch (IllegalArgumentException | ValidationException ex) {
      throw ConnectorException.newBuilder("Browse Failed: " + ex.getMessage(), ex)
          .setFailureReason(ConnectorException.FailureReason.INVALID_ARGUMENT)
          .build();
    } finally {
      ConnectorMetricsUtil.recordApiCountAndLatency(
          openTelemetry,
          dataSourceId,
          API_BROWSE_ASSET,
          status,
          Duration.between(start, Instant.now()));
    }
  }
}
