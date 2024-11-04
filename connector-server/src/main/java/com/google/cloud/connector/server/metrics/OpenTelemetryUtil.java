package com.google.cloud.connector.server.metrics;

import static com.google.cloud.connector.server.metrics.MetricsConstants.ATTRIBUTE_API_NAME;
import static com.google.cloud.connector.server.metrics.MetricsConstants.ATTRIBUTE_DATASOURCE;
import static com.google.cloud.connector.server.metrics.MetricsConstants.ATTRIBUTE_STATUS;
import static com.google.cloud.connector.server.metrics.MetricsConstants.METRIC_API_COUNT;
import static com.google.cloud.connector.server.metrics.MetricsConstants.METRIC_API_LATENCY;
import static com.google.cloud.connector.server.metrics.MetricsConstants.METRIC_BYTES_READ;
import static com.google.cloud.connector.server.metrics.MetricsConstants.METRIC_ROWS_READ;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;

/**
 * OpenTelemetry Utility Class.
 */
public final class OpenTelemetryUtil {

  /**
   * Build API counter.
   */
  public static LongCounter getApiCounter(Meter meter) {
    return meter
        .counterBuilder(METRIC_API_COUNT)
        .setDescription("API response code")
        .setUnit("cnt")
        .build();
  }

  /**
   * Build latency histogram.
   *
   * @param meter Meter
   * @return latency histogram of longs
   */
  public static LongHistogram getLatencyHistogram(Meter meter) {
    return meter.histogramBuilder(METRIC_API_LATENCY).ofLongs().build();
  }

  /**
   * Build histogram for distribution based metrics.
   *
   * @return long histogram
   */
  public static LongHistogram getHistogram(Meter meter, String name) {
    return meter.histogramBuilder(name).ofLongs().build();
  }

  /**
   * Build row counter.
   */
  public static LongCounter getRowCounter(Meter meter) {
    return meter
        .counterBuilder(METRIC_ROWS_READ)
        .setDescription("API row count")
        .setUnit("cnt")
        .build();
  }

  /**
   * Build bytes read counter.
   */
  public static LongCounter getBytesReadCounter(Meter meter) {
    return meter
        .counterBuilder(METRIC_BYTES_READ)
        .setDescription("API bytes read")
        .setUnit("bytes")
        .build();
  }

  /**
   * Gets OpenTelemetry attributes.
   *
   * @param apiName API name
   * @return API name attribute
   */
  public static Attributes getAttributes(String datasourceId, String apiName) {
    return Attributes.of(
        AttributeKey.stringKey(ATTRIBUTE_DATASOURCE), datasourceId,
        AttributeKey.stringKey(ATTRIBUTE_API_NAME), apiName);
  }

  /**
   * Gets OpenTelemetry attributes.
   *
   * @param apiName API name
   * @param status response code (success or failed)
   * @return API name and Status Code attributes
   */
  public static Attributes getAttributes(String datasourceId, String apiName, String status) {
    return Attributes.of(
        AttributeKey.stringKey(ATTRIBUTE_DATASOURCE), datasourceId,
        AttributeKey.stringKey(ATTRIBUTE_API_NAME), apiName,
        AttributeKey.stringKey(ATTRIBUTE_STATUS), status);
  }
}
