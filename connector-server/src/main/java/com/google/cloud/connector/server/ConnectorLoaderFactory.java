package com.google.cloud.connector.server;

/** A factory for creating {@link ConnectorLoader}. */
public interface ConnectorLoaderFactory {

  /**
   * Returns an instance of {@link ConnectorLoader} for the given data source.
   *
   * @param datasourceId the datasource identifier
   */
  ConnectorLoader get(String datasourceId);
}
