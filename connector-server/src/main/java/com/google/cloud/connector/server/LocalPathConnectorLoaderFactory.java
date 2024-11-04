package com.google.cloud.connector.server;

import com.google.inject.Inject;

/**
 * A straightforward implementation of {@link ConnectorLoaderFactory} that creates {@link
 * LocalPathConnectorLoader} directly.
 */
public class LocalPathConnectorLoaderFactory implements ConnectorLoaderFactory {

  private final ConnectorLoaderConfig config;

  @Inject
  LocalPathConnectorLoaderFactory(ConnectorLoaderConfig config) {
    this.config = config;
  }

  @Override
  public ConnectorLoader get(String datasourceId) {
    return new LocalPathConnectorLoader(config.getConnectorRootDir().resolve(datasourceId));
  }
}
