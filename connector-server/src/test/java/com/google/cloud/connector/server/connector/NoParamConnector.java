package com.google.cloud.connector.server.connector;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.schema.SchemaBuilder;

/** A connector that does nothing. It's for testing the no parameter data source. */
public class NoParamConnector implements Connector, SynchronousQueryExecutor {

  @DataSource
  public NoParamConnector() {
    // no-op
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return NoParamConnector.class.getName();
  }
}
