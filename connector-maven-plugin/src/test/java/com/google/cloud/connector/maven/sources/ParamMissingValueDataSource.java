package com.google.cloud.connector.maven.sources;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.annotation.Parameter;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import java.io.IOException;

/** A data source with a parameter that misses the value. */
public class ParamMissingValueDataSource implements Connector, SynchronousQueryExecutor {

  private final String uri;

  @DataSource
  public ParamMissingValueDataSource(@Parameter(value = "") String uri) {
    this.uri = uri;
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {}

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) throws IOException {
    return null;
  }
}
