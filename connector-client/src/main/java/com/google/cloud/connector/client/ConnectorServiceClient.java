package com.google.cloud.connector.client;

import com.google.cloud.bigquery.federation.v1alpha1.ConnectorServiceGrpc;
import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.bigquery.federation.v1alpha1.DataSource;
import com.google.cloud.bigquery.federation.v1alpha1.ExecuteQueryRequest;
import com.google.cloud.bigquery.federation.v1alpha1.GetDataSourceRequest;
import com.google.cloud.bigquery.federation.v1alpha1.NamedTable;
import com.google.cloud.bigquery.federation.v1alpha1.ResolveSchemaRequest;
import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryRequest;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse;
import com.google.protobuf.Struct;
import io.grpc.ManagedChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Client implementation which uses {@link ConnectorServiceGrpc} to interact with the Connector
 * Service.
 *
 * <p>Implements {@link AutoCloseable} to manage GRPC channel lifecycle.
 */
public class ConnectorServiceClient implements AutoCloseable {
  private final String connectorName;
  private final Struct connectorParameters;
  private final ManagedChannel channel;
  private final ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub;

  /**
   * Constructor.
   *
   * @param channel             GRPC managed channel
   * @param connectorName       Connector Name
   * @param connectorParameters {@link Struct} containing the datasource-type-specific parameters
   *                            for this connector
   */
  public ConnectorServiceClient(
      ManagedChannel channel, String connectorName, Struct connectorParameters) {
    this.channel = channel;
    this.connectorName = connectorName;
    this.connectorParameters = connectorParameters;
    this.blockingStub = ConnectorServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Get Data Source information for this Connector.
   *
   * @return {@link DataSource} instance with details for this connector.
   */
  public DataSource getDataSource() {
    GetDataSourceRequest request = GetDataSourceRequest.newBuilder().setName(
        String.format("datasources/%s", connectorName)).build();
    return blockingStub.getDataSource(request);
  }

  /**
   * Resolve schema for a specified Dataset and Table.
   *
   * @param dataset    dataset name
   * @param namedTable table name
   * @param nativeQuery native query
   * @return {@link Schema} for the specified dataset and table
   */
  public Schema resolveSchema(String dataset,
      @Nullable NamedTable namedTable,
      @Nullable String nativeQuery) {
    ResolveSchemaRequest.Builder requestBuilder =
        ResolveSchemaRequest.newBuilder().setDataset(dataset).setParameters(connectorParameters);

    if (namedTable != null) {
      requestBuilder.setNamedTable(namedTable);
    }

    if (nativeQuery != null) {
      requestBuilder.setNativeQuery(nativeQuery);
    }

    return blockingStub.resolveSchema(requestBuilder.build());
  }

  /**
   * Start a read session for a specified dataset and table.
   *
   * @param dataset dataset name
   * @param namedTable named table
   * @param nativeQuery native query
   * @return {@link StartQueryResponse} containing details about the result set and streams to read
   *     from.
   */
  public StartQueryResponse startQuery(String dataset,
      @Nullable NamedTable namedTable,
      @Nullable String nativeQuery) {
    DataQuery.Builder dataQueryBuilder = DataQuery.newBuilder();

    if (namedTable != null) {
      dataQueryBuilder.setNamedTable(namedTable);
    }

    if (nativeQuery != null) {
      dataQueryBuilder.setNativeQuery(nativeQuery);
    }

    StartQueryRequest.Builder requestBuilder =
        StartQueryRequest.newBuilder()
            .setDataset(dataset)
            .setDataQuery(dataQueryBuilder.build())
            .setParameters(connectorParameters);

    return blockingStub.startQuery(requestBuilder.build());
  }

  /**
   * Executes a query synchronously for a specified dataset and table.
   *
   * @param dataset     dataset name
   * @param nativeQuery native query (optional)
   * @param namedTable  fully qualified table name (optional)
   * @return {@link Iterator} used to consume the results of this query. All data will be returned
   *     in a single stream, and this iterator may be used to consume the results.
   */
  public Iterator<Data> executeQuery(String dataset,
      @Nullable NamedTable namedTable,
      @Nullable String nativeQuery) {
    DataQuery.Builder dataQueryBuilder = DataQuery.newBuilder();

    if (namedTable != null) {
      dataQueryBuilder.setNamedTable(namedTable);
    }

    if (nativeQuery != null) {
      dataQueryBuilder.setNativeQuery(nativeQuery);
    }

    ExecuteQueryRequest.Builder requestBuilder =
        ExecuteQueryRequest.newBuilder().setDataset(dataset).setParameters(connectorParameters);
    requestBuilder.setDataQuery(dataQueryBuilder.build());

    return blockingStub.executeQuery(requestBuilder.build());
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