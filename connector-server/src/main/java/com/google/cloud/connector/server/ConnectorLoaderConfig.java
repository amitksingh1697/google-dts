package com.google.cloud.connector.server;

import static picocli.CommandLine.Help.Visibility.ALWAYS;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import picocli.CommandLine.Option;

/** Configuration class for the {@link ConnectorLoader}. */
public class ConnectorLoaderConfig {

  @Option(
      names = {"-r", "--connector-root-dir"},
      required = true,
      description =
          "The top level root directory for all connector jars. "
              + "Each connector should be in a subdirectory by itself under this root dir. "
              + "The subdirectory name should match the connector's datasourceId.")
  private String connectorRootDir;

  @Option(
      names = {"--enable-loader-cache"},
      description = "Enable connector loader cache.",
      defaultValue = "true",
      showDefaultValue = ALWAYS)
  private boolean enableCache;

  @SuppressWarnings("unused")
  public ConnectorLoaderConfig() {
    // no-op, for picocli to use.
  }

  @VisibleForTesting
  ConnectorLoaderConfig(String connectorRootDir, boolean enableCache) {
    this.connectorRootDir = connectorRootDir;
    this.enableCache = enableCache;
  }

  public Path getConnectorRootDir() {
    return Path.of(connectorRootDir);
  }

  public boolean isEnableCache() {
    return enableCache;
  }
}
