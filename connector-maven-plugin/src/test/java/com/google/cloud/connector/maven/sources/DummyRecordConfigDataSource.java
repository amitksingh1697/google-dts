package com.google.cloud.connector.maven.sources;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.schema.SchemaBuilder;

/** A no-op data source for testing the maven mojo that uses a record as config class. */
public class DummyRecordConfigDataSource implements Connector, SynchronousQueryExecutor {

  private final DummyRecordConfig config;

  @DataSource(value = "dummy", maxStalenessMillis = 7200100L)
  public DummyRecordConfigDataSource(DummyRecordConfig config) {
    this.config = config;
  }

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }
}
