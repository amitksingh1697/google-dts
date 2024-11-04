package com.google.cloud.connector.maven.sources;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.schema.SchemaBuilder;

/** A data source that doesn't take parameter. */
public class NoParamDataSource implements Connector, SynchronousQueryExecutor {

  @DataSource
  public NoParamDataSource() {}

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }
}
