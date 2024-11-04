package com.google.cloud.connector.server.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * OpenTelemetry provider.
 */
public class OpenTelemetryProvider {
  static SdkMeterProvider meterProvider;

  private static final String UCP_AGENT_HOSTNAME = "ucp-agent";
  private static final int UCP_AGENT_PORT = 4317;

  private static MetricExporter metricExporter;

  /**
   * Creates an OpenTelemetry instance.
   */
  public static OpenTelemetry getOpenTelemetrySdk(MetricExporter exporter) {
    Resource resource = Resource.getDefault().toBuilder().build();

    if (meterProvider == null) {
      meterProvider =
          SdkMeterProvider.builder()
              .registerMetricReader(
                  PeriodicMetricReader.builder(exporter).setInterval(1, TimeUnit.MINUTES).build())
              .setResource(resource)
              .build();
    }

    OpenTelemetrySdk openTelemetrySdk =
        OpenTelemetrySdk.builder().setMeterProvider(meterProvider).buildAndRegisterGlobal();
    Runtime.getRuntime().addShutdownHook(new Thread(openTelemetrySdk::close));
    return openTelemetrySdk;
  }

  /**
   * Retrieves Metric Exporter if not defined.
   *
   * @return OtlpGrpcMetricExporter
   */
  public static MetricExporter getOltpGrpcExporter() {
    if (metricExporter == null) {
      String grpcEndpoint =
          String.format("http://%s:%s", getUcpAgentHostAddress(), UCP_AGENT_PORT);
      metricExporter = OtlpGrpcMetricExporter.builder().setEndpoint(grpcEndpoint).build();
    }
    return metricExporter;
  }

  /**
   * Get the host IP address of UCP agent running on the same network. Defaults to 'localhost'
   * if UCP agent fails to startup before connector server. <br><br>
   * Note: If UCP agent container's IP address is changed when connector server is already running,
   * the server will still use the old IP and will lead to export errors.
   *
   * @return IP address of the UCP agent container
   */
  private static String getUcpAgentHostAddress() {
    String ucpAgentHostAddress;
    try {
      ucpAgentHostAddress = InetAddress.getByName(UCP_AGENT_HOSTNAME).getHostAddress();
    } catch (UnknownHostException e) {
      ucpAgentHostAddress = "localhost";
    }
    return ucpAgentHostAddress;
  }
}
