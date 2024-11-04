package com.google.cloud.connector.client;

import com.google.cloud.bigquery.federation.v1alpha1.NamedTable;
import com.google.cloud.connector.api.BrowseAsset;
import com.google.cloud.connector.api.BrowseRequest;
import com.google.cloud.connector.api.BrowseResponse;
import com.google.cloud.connector.api.DataExploreServiceGrpc;
import com.google.protobuf.Struct;
import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;

/**
 * Client implementation which uses {@link DataExploreServiceGrpc} to interact with the
 * Data Explore Service.
 *
 * <p>Implements {@link AutoCloseable} to manage GRPC channel lifecycle.
 */
public class DataExploreServiceClient implements AutoCloseable {
  private final Struct connectorParameters;
  private final ManagedChannel channel;
  private final DataExploreServiceGrpc.DataExploreServiceBlockingStub blockingStub;

  /**
   * Constructor.
   *
   * @param channel             GRPC managed channel
   * @param connectorParameters {@link Struct} containing the datasource-type-specific parameters
   *                            for this connector
   */
  public DataExploreServiceClient(Struct connectorParameters, ManagedChannel channel) {
    this.connectorParameters = connectorParameters;
    this.channel = channel;
    this.blockingStub = DataExploreServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Browse the given dataset and get the child datasets in it.
   *
   * @param dataset dataset name
   * @param namedTable named table
   * @return the browse response
   */
  public BrowseResponse browse(String dataset, NamedTable namedTable) {
    return blockingStub.browse(
        BrowseRequest.newBuilder()
            .setAsset(
                BrowseAsset.newBuilder().setDataset(dataset).setNamedTable(namedTable))
            .setParameters(connectorParameters)
            .build());
  }

  @Override
  public void close() {
    // Close the GRPC channel after completion.
    try {
      channel.shutdown();
      channel.awaitTermination(5, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted Exception when waiting for GRPC channel to close", e);
    }
  }
}