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

/** A no-op data source for testing the maven mojo that doesn't use configuration class. */
public class DirectConfigDataSource implements Connector, SynchronousQueryExecutor {

  private final String source;
  private final double threshold;

  @DataSource
  public DirectConfigDataSource(
      @Parameter("source") String source, @Parameter("threshold") double threshold) {
    this.source = source;
    this.threshold = threshold;
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
