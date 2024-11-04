package com.google.cloud.connector.maven.sources;

import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_SYNCHRONOUS_QUERIES;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.ParallelQueryExecutor;
import com.google.cloud.connector.api.ParallelQueryPreparationContext;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import java.io.IOException;

/** A no-op data source for testing the maven mojo that uses a regular class as config class. */
public class SimpleDataSource implements Connector, ParallelQueryExecutor {

  private final SimpleConfig config;

  @DataSource(capabilities = {SUPPORTS_SYNCHRONOUS_QUERIES})
  public SimpleDataSource(SimpleConfig config) {
    this.config = config;
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void prepareQuery(
      AssetName assetName, DataQuery query, ParallelQueryPreparationContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordReader readPartition(AssetName assetName, byte[] partitionData) throws IOException {
    throw new UnsupportedOperationException();
  }
}
