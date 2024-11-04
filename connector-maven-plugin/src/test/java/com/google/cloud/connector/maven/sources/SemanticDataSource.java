package com.google.cloud.connector.maven.sources;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import java.io.IOException;

/** A datasource for testing semantic type configuration. */
public class SemanticDataSource implements Connector, SynchronousQueryExecutor {

  private final SemanticConfig config;

  @DataSource
  public SemanticDataSource(SemanticConfig config) {
    this.config = config;
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) throws IOException {
    throw new UnsupportedOperationException();
  }
}
