package com.google.cloud.connector.server;

import com.google.cloud.connector.api.discovery.EndpointRegistry;
import com.google.cloud.connector.gcp.ServiceDirectoryEndpointConfig;
import com.google.cloud.connector.gcp.ServiceDirectoryEndpointRegistry;
import com.google.cloud.connector.server.metrics.OpenTelemetryProvider;
import com.google.common.flogger.FluentLogger;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerBuilder;
import io.grpc.alts.AltsServerCredentials;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/** A guice module for providing bindings for the {@link ConnectorServer}. */
final class ConnectorModule extends PrivateModule {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Option(
      names = {"-p", "--port"},
      required = true,
      description = "TCP port for the server to bind to.")
  private int port;

  @Option(
      names = {"-k", "--insecure"},
      description =
          "By default, ATLS will be used. This option will turn the server "
              + "to accept any request without authentication.")
  private boolean insecure;

  @Option(
      names = {"-s", "--substrait"},
      description = "Use Substrait schema.")
  private boolean useSubstrait;

  @Option(
      names = {"-ac", "--allowed-service-accounts"},
      description =
          "A comma separated list of allowed clients that an communicate "
              + "with connector server.",
      converter = AllowedServiceAccountListConverter.class)
  private List<String> allowedServiceAccounts;

  @Option(
      names = {"-m", "--enable-metrics"},
      description = "By default, metrics are not pushed. This option will turn on pushing metrics.")
  private boolean enableMetrics;

  @Mixin private ServiceDirectoryEndpointConfig serviceDirectoryConfig;
  @Mixin private ConnectorLoaderConfig connectorLoaderConfig;

  @Override
  protected void configure() {
    if (insecure) {
      logger.atWarning().log("Requested to execute in insecure mode");
    }

    ServerBuilder<?> serverBuilder =
        Grpc.newServerBuilderForPort(
            port, insecure ? InsecureServerCredentials.create() : AltsServerCredentials.create());

    if (allowedServiceAccounts != null && !allowedServiceAccounts.isEmpty()) {
      serverBuilder.intercept(new AuthConnectorServerInterceptor(allowedServiceAccounts));
    }
    bind(new TypeLiteral<ServerBuilder<?>>() {}).toInstance(serverBuilder);

    bind(ConnectorLoaderConfig.class).toInstance(connectorLoaderConfig);
    if (connectorLoaderConfig.isEnableCache()) {
      install(
          new PrivateModule() {
            @Override
            protected void configure() {
              bind(ConnectorLoaderFactory.class)
                  .annotatedWith(CachingConnectorLoaderFactory.InternalFactory.class)
                  .to(LocalPathConnectorLoaderFactory.class);
              bind(ConnectorLoaderFactory.class)
                  .to(CachingConnectorLoaderFactory.class)
                  .in(Scopes.SINGLETON);
              expose(ConnectorLoaderFactory.class);
            }
          });
    } else {
      bind(ConnectorLoaderFactory.class).to(LocalPathConnectorLoaderFactory.class);
    }

    bind(EndpointRegistry.class).toInstance(createEndpointRegistry());

    if (useSubstrait) {
      bind(DataSchemaBuilder.class).to(SubstraitDataSchemaBuilder.class);
    } else {
      bind(DataSchemaBuilder.class).to(ConnectorDataSchemaBuilder.class);
    }

    Multibinder<BindableService> serviceBinder =
        Multibinder.newSetBinder(binder(), BindableService.class);
    serviceBinder.addBinding().to(ConnectorService.class);
    serviceBinder.addBinding().to(ReaderService.class);
    serviceBinder.addBinding().to(DataExploreService.class);

    bind(ConnectorServer.class);
    expose(ConnectorServer.class);

    bind(OpenTelemetry.class).toInstance(createOpenTelemetryInstance());
  }

  private EndpointRegistry createEndpointRegistry() {
    return serviceDirectoryConfig.isConfigured()
        ? new ServiceDirectoryEndpointRegistry(serviceDirectoryConfig)
        : EndpointRegistry.NOOP;
  }

  private static final class AllowedServiceAccountListConverter
      implements ITypeConverter<List<String>> {
    @Override
    public List<String> convert(String allowedClients) throws Exception {
      return Stream.of(allowedClients.split(",")).collect(Collectors.toList());
    }
  }

  private OpenTelemetry createOpenTelemetryInstance() {
    if (!enableMetrics) {
      return OpenTelemetry.noop();
    }
    MetricExporter metricExporter = OpenTelemetryProvider.getOltpGrpcExporter();
    return OpenTelemetryProvider.getOpenTelemetrySdk(metricExporter);
  }
}
