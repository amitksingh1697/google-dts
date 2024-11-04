package com.google.cloud.connector.server.metrics;

/**
 * All constants used for connector server metrics.
 */
public class MetricsConstants {

  public static final String METER_NAME = "otelMeter";

  /**
   * Constants for metrics attributes.
   */
  public static final String ATTRIBUTE_API_NAME = "method";
  public static final String ATTRIBUTE_STATUS = "error_code";
  public static final String ATTRIBUTE_DATASOURCE = "datasource_id";

  /**
   * Constants for Api names.
   */
  public static final String API_RESOLVE_SCHEMA = "ResolveSchema";
  public static final String API_START_QUERY = "StartQuery";
  public static final String API_EXECUTE_QUERY = "ExecuteQuery";
  public static final String API_READ_STREAM = "ReadStream";
  public static final String API_BROWSE_ASSET = "BrowseAsset";

  /**
   * Constants for Api status.
   */
  public static final String API_STATUS_SUCCESS = "success";
  public static final String API_STATUS_FAILED = "failed";
  public static final String API_STATUS_RUNNING = "running";

  /**
   * Constants for metric names.
   */
  public static final String METRIC_API_COUNT = "request_count";
  public static final String METRIC_API_LATENCY = "request_latencies";
  public static final String METRIC_ROWS_READ = "rows_read_count";
  public static final String METRIC_BYTES_READ = "bytes_read_count";
  public static final String METRIC_THROUGHPUT = "throughput_count";
}
