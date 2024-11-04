package com.google.cloud.connector.client;

/**
 * Helper class for generating load test statistics.
 */
public class LoadTester {
  public static final int NUM_RUNS = 2;

  /**
   * Write load testing statistics.
   *
   * @param stats {@link Statistics} to write
   */
  public static void writeStats(Statistics stats) {
    // TODO support writing to a file
    System.out.println(stats.toString());
  }

  /**
   * Load test result statistics.
   *
   * @param dataSize      size of data processed
   * @param latencyTime   time between request and response
   * @param rowsRead      number of rows read (ExecuteQuery and ReadPartition)
   * @param totalDataSize total size of data processed (ExecuteQuery and ReadPartition)
   * @param totalTime     total time to execute request (ExecuteQuery and ReadPartition)
   */
  public record Statistics(double dataSize, long latencyTime, long rowsRead, double totalDataSize,
                           long totalTime) {

    public static Builder getBuilder(double dataSize, long latencyTime) {
      return new Builder(dataSize, latencyTime);
    }

    @Override
    public String toString() {
      // Data size is printed in MB
      return String.format("%f,%d,%d,%f,%d", dataSize / 1_048_576, latencyTime, rowsRead,
          totalDataSize / 1_048_576, totalTime);
    }

    /**
     * Builder class for {@link Statistics}.
     */
    public static final class Builder {
      private final long latencyTime;
      private final double dataSize;
      private double totalDataSize;
      private long totalTime;
      private long rowsRead;

      public Builder(double dataSize, long latencyTime) {
        this.dataSize = dataSize;
        this.latencyTime = latencyTime;
      }

      public Builder setRowsRead(long rowsRead) {
        this.rowsRead = rowsRead;
        return this;
      }

      public Builder setTotalTime(long totalTime) {
        this.totalTime = totalTime;
        return this;
      }

      public Builder setTotalDataSize(double totalDataSize) {
        this.totalDataSize = totalDataSize;
        return this;
      }

      public Statistics build() {
        return new Statistics(dataSize, latencyTime, rowsRead, totalDataSize, totalTime);
      }
    }
  }
}
