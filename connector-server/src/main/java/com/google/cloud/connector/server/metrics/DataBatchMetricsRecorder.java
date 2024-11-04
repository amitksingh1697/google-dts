package com.google.cloud.connector.server.metrics;

import static com.google.cloud.connector.server.metrics.MetricsConstants.API_STATUS_RUNNING;
import static com.google.cloud.connector.server.metrics.MetricsConstants.METER_NAME;
import static com.google.cloud.connector.server.metrics.MetricsConstants.METRIC_THROUGHPUT;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import java.time.Duration;
import java.time.Instant;

/**
 * Wrapper class to encapsulate information required to send row count, bytes read, throughput and
 * latency metrics.
 */
public class DataBatchMetricsRecorder {
  private final OpenTelemetry openTelemetry;
  private final String datasourceId;
  private final String apiName;
  private final Instant startTime;

  // To record throughput across multiple batches
  private final Throughput averageThroughput;
  private final ObservableDoubleGauge throughputGauge;

  /**
   * Constructs new {@link DataBatchMetricsRecorder}.
   *
   * @param openTelemetry {@link OpenTelemetry}
   * @param datasourceId Datasource ID
   * @param apiName Api Name
   * @param startTime {@link Instant}
   */
  public DataBatchMetricsRecorder(
      OpenTelemetry openTelemetry, String datasourceId, String apiName, Instant startTime) {
    this.openTelemetry = openTelemetry;
    this.datasourceId = datasourceId;
    this.apiName = apiName;
    this.startTime = startTime;
    this.throughputGauge = getThroughputGauge(openTelemetry.getMeter(METER_NAME));
    this.averageThroughput = new Throughput();
  }

  /**
   * Records gauge metrics for row count, bytes read and throughput.
   *
   * @param rowCount {@link Integer}
   * @param bytesRead {@link Long}
   * @param duration {@link Duration}
   */
  public void recordMetrics(Integer rowCount, Long bytesRead, Duration duration) {
    ConnectorMetricsUtil.recordRowCount(openTelemetry, datasourceId, apiName, rowCount);
    ConnectorMetricsUtil.recordBytesReadCount(openTelemetry, datasourceId, apiName, bytesRead);
    double throughput = ((double) bytesRead) / ((double) duration.toNanos() / 1_000_000_000);
    averageThroughput.setValue(averageThroughput.getValue() == 0
        ? throughput : (throughput + averageThroughput.getValue()) / 2);
    averageThroughput.setRecorded(false);
  }

  /**
   * Record latency metric for Read Stream and Execute Query. Usage: used to send latency from start
   * of api to first batch of data sent.
   */
  public void recordLatency() {
    ConnectorMetricsUtil.recordLatency(
        openTelemetry,
        datasourceId,
        apiName,
        API_STATUS_RUNNING,
        Duration.between(startTime, Instant.now()));
  }

  /**
   * Build throughput gauge.
   */
  private ObservableDoubleGauge getThroughputGauge(Meter meter) {
    return meter
        .gaugeBuilder(METRIC_THROUGHPUT)
        .setDescription("API throughput")
        .setUnit("bytes/second")
        .buildWithCallback(this::recordAverageThroughput);
  }

  /**
   * Record the average throughput value.
   */
  private void recordAverageThroughput(ObservableDoubleMeasurement measurement) {
    if (!averageThroughput.isRecorded()) {
      measurement.record(averageThroughput.getValue(),
          OpenTelemetryUtil.getAttributes(datasourceId, apiName));
      averageThroughput.setRecorded(true);
    }
  }

  /**
   * Stores information about throughput value and whether it was recorded already.
   */
  private class Throughput {
    private double value;
    private boolean isRecorded;

    public Throughput() {
      this.value = 0;
      this.isRecorded = false;
    }

    public double getValue() {
      return value;
    }

    public boolean isRecorded() {
      return isRecorded;
    }

    public void setValue(double value) {
      this.value = value;
    }

    public void setRecorded(boolean isRecorded) {
      this.isRecorded = isRecorded;
    }
  }
}
