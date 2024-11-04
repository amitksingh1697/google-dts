package com.google.cloud.connector.server.connector;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.config.HostAndPort;
import com.google.cloud.connector.api.config.oauth.OauthClientId;
import com.google.cloud.connector.api.schema.SchemaBuilder;

/** Connector to test connector loading. */
public class TestConnector implements Connector, SynchronousQueryExecutor {

  private final Config config;

  @DataSource
  public TestConnector(Config config) {
    this.config = config;
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) {
    throw new UnsupportedOperationException();
  }

  /** Config class. */
  public record Config(
      HostAndPort hostAndPort, OauthClientId oauthClientId, double threshold, Long timeout) {}

  @Override
  public String toString() {
    return String.format(
        "%s,%s,%d,%s,%.2f,%d",
        getClass().getName(),
        config.hostAndPort().host(),
        config.hostAndPort().port(),
        config.oauthClientId.value(),
        config.threshold(),
        config.timeout());
  }
}