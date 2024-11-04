package com.google.cloud.connector.server.metrics;

import static com.google.cloud.connector.server.metrics.MetricsConstants.METER_NAME;
import static com.google.cloud.connector.server.metrics.OpenTelemetryUtil.getApiCounter;
import static com.google.cloud.connector.server.metrics.OpenTelemetryUtil.getAttributes;
import static com.google.cloud.connector.server.metrics.OpenTelemetryUtil.getBytesReadCounter;
import static com.google.cloud.connector.server.metrics.OpenTelemetryUtil.getLatencyHistogram;
import static com.google.cloud.connector.server.metrics.OpenTelemetryUtil.getRowCounter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import java.time.Duration;

/** Util functions for recording connector server metrics. */
public class ConnectorMetricsUtil {
  /**
   * Records Api count metrics.
   *
   * @param openTelemetry {@link OpenTelemetry}
   * @param datasourceId Datasource ID
   * @param apiName Api name
   * @param status Status of Api
   */
  public static void recordApiCount(
      OpenTelemetry openTelemetry, String datasourceId, String apiName, String status) {
    Meter meter = openTelemetry.getMeter(METER_NAME);
    recordApiCount(meter, getAttributes(datasourceId, apiName, status), 1);
  }

  private static void recordApiCount(Meter meter, Attributes attributes, long count) {
    LongCounter apiCounter = getApiCounter(meter);
    apiCounter.add(count, attributes);
  }

  /**
   * Records Api latency metrics.
   *
   * @param openTelemetry {@link OpenTelemetry}
   * @param datasourceId Datasource ID
   * @param apiName Api name
   * @param status Status of Api
   * @param duration time taken to execute the api
   */
  public static void recordLatency(
      OpenTelemetry openTelemetry,
      String datasourceId,
      String apiName,
      String status,
      Duration duration) {
    Meter meter = openTelemetry.getMeter(METER_NAME);
    recordLatency(meter, getAttributes(datasourceId, apiName, status), duration.toMillis());
  }

  private static void recordLatency(Meter meter, Attributes attributes, long latency) {
    LongHistogram latencyHistogram = getLatencyHistogram(meter);
    latencyHistogram.record(latency, attributes);
  }

  /**
   * Records Api count and latency metrics.
   *
   * @param openTelemetry {@link OpenTelemetry}
   * @param datasourceId Datasource ID
   * @param apiName Api name
   * @param status Status of Api
   * @param duration time taken to execute the api
   */
  public static void recordApiCountAndLatency(
      OpenTelemetry openTelemetry,
      String datasourceId,
      String apiName,
      String status,
      Duration duration) {
    Meter meter = openTelemetry.getMeter(METER_NAME);
    recordApiCount(meter, getAttributes(datasourceId, apiName, status), 1);
    recordLatency(meter, getAttributes(datasourceId, apiName, status), duration.toMillis());
  }

  /**
   * Records rows read count metric.
   *
   * @param openTelemetry {@link OpenTelemetry}
   * @param datasourceId  Datasource ID
   * @param apiName       API name
   * @param rowCount      Number of rows
   */
  public static void recordRowCount(
      OpenTelemetry openTelemetry,
      String datasourceId,
      String apiName,
      long rowCount
  ) {
    Meter meter = openTelemetry.getMeter(METER_NAME);
    recordRowCount(meter, getAttributes(datasourceId, apiName), rowCount);
  }

  private static void recordRowCount(Meter meter, Attributes attributes, long rowCount) {
    LongCounter rowCounter = getRowCounter(meter);
    rowCounter.add(rowCount, attributes);
  }

  /**
   * Records bytes read count metric.
   *
   * @param openTelemetry {@link OpenTelemetry}
   * @param datasourceId  Datasource ID
   * @param apiName       API name
   * @param bytesRead     Number of bytes read
   */
  public static void recordBytesReadCount(
      OpenTelemetry openTelemetry,
      String datasourceId,
      String apiName,
      long bytesRead) {
    Meter meter = openTelemetry.getMeter(METER_NAME);
    recordBytesReadCount(meter, getAttributes(datasourceId, apiName), bytesRead);
  }

  private static void recordBytesReadCount(Meter meter, Attributes attributes, long bytesRead) {
    LongCounter bytesReadCounter = getBytesReadCounter(meter);
    bytesReadCounter.add(bytesRead, attributes);
  }
}
