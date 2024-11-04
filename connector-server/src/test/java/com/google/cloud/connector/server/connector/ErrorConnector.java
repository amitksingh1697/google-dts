package com.google.cloud.connector.server.connector;

import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_PARALLEL_QUERIES;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_SYNCHRONOUS_QUERIES;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.BrowseAsset;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.DataExplorer;
import com.google.cloud.connector.api.ParallelQueryExecutor;
import com.google.cloud.connector.api.ParallelQueryPreparationContext;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.browse.BrowseAssetResult;
import com.google.cloud.connector.api.exception.ConnectorException;
import com.google.cloud.connector.api.exception.ConnectorException.FailureReason;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import java.io.IOException;

/** A connector for a simple in-memory database * */
public class ErrorConnector
    implements SynchronousQueryExecutor, Connector, ParallelQueryExecutor, DataExplorer {
  public static final String DATASOURCE_ID = "inmemerror";
  public static final String DATABASE_NAME = "inMemDatabase";

  public static final String COLLECTION_ID_TABLE = "tables";

  public static final String CONFIG_KEY_NUM_ROWS = "numRows";

  public static final String TABLE_NAME_PERSON = "person";
  public static final String TABLE_NAME_CUSTOMER = "customer";

  public static final int QUERY_MAX_CONCURRENCY = 1;

  public static final String DATABASE_COLLECTION = "databases";
  public static final String TABLE_COLLECTION = "tables";

  public static final BrowseAsset DATABASE_ASSET =
      BrowseAsset.newBuilder()
          .setDataset(String.join("/", DATABASE_COLLECTION, DATABASE_NAME))
          .setDisplayName(DATABASE_NAME)
          .build();

  @DataSource(
      value = DATASOURCE_ID,
      capabilities = {SUPPORTS_SYNCHRONOUS_QUERIES, SUPPORTS_PARALLEL_QUERIES})
  public ErrorConnector() {
    // no-op
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    // Throw Connector Exception with an INVALID_ARGUMENT failure reason
    throw ConnectorException.newBuilder("error", new RuntimeException("oof"))
        .setFailureReason(FailureReason.INVALID_ARGUMENT)
        .build();
  }

  @Override
  public BrowseAssetResult browseAsset(AssetName parentAssetName) {
    // Throw Connector Exception with an INVALID_CREDENTIALS failure reason
    throw ConnectorException.newBuilder("error", new RuntimeException("oof"))
        .setFailureReason(FailureReason.PERMISSION_DENIED)
        .build();
  }

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) {
    // Throw Connector Exception with an INTERNAL failure reason
    throw ConnectorException.newBuilder("error", new RuntimeException("oof"))
        .setFailureReason(FailureReason.INTERNAL)
        .build();
  }

  @Override
  public void prepareQuery(
      AssetName assetName, DataQuery query, ParallelQueryPreparationContext context) {
    // Throw Connector Exception with an SERVICE_UNAVAILABLE failure reason
    throw ConnectorException.newBuilder("error", new RuntimeException("oof"))
        .setFailureReason(FailureReason.SERVICE_UNAVAILABLE)
        .build();
  }

  @Override
  public RecordReader readPartition(AssetName assetName, byte[] partitionData) throws IOException {
    // Throw Connector Exception with an SERVICE_UNAVAILABLE failure reason
    throw ConnectorException.newBuilder("error", new RuntimeException("oof"))
        .setFailureReason(FailureReason.SERVICE_UNAVAILABLE)
        .build();
  }

  @Override
  public String toString() {
    return String.format("ConnectorName: '%s'", getClass().getName());
  }
}
