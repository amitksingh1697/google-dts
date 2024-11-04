package com.google.cloud.connector.server;

import com.google.cloud.connector.api.discovery.EndpointRegistry;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.Closeable;
import java.util.Set;

/**
 * A guava {@link com.google.common.util.concurrent.Service} for managing the life cycle of a grpc
 * server for the connector service.
 */
class ConnectorServer extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Server server;
  private final EndpointRegistry endpointRegistry;
  private final ConnectorLoaderFactory connectorLoaderFactory;
  private Closeable serviceRegistration;

  @Inject
  ConnectorServer(
      ServerBuilder<?> serverBuilder,
      EndpointRegistry endpointRegistry,
      Set<BindableService> services,
      ConnectorLoaderFactory connectorLoaderFactory) {

    this.server =
        services.stream()
            .reduce(
                serverBuilder,
                ServerBuilder::addService,
                (unused1, unused2) -> {
                  throw new UnsupportedOperationException();
                })
            .intercept(new ExceptionHandler())
            .build();

    this.endpointRegistry = endpointRegistry;
    this.connectorLoaderFactory = connectorLoaderFactory;
  }

  public int getPort() {
    return server.getPort();
  }

  @Override
  protected void startUp() throws Exception {
    server.start();
    serviceRegistration = endpointRegistry.register(getPort());
  }

  @Override
  protected void shutDown() throws InterruptedException {
    try {
      if (serviceRegistration != null) {
        serviceRegistration.close();
      }
    } catch (Exception e) {
      logger.atWarning().withCause(e).log("Failed to unregister service");
    }
    server.shutdown();
    server.awaitTermination();

    if (connectorLoaderFactory instanceof Closeable) {
      try {
        ((Closeable) connectorLoaderFactory).close();
      } catch (Exception e) {
        logger.atWarning().withCause(e).log("Failed to close connector loader factory");
      }
    }
  }
}
