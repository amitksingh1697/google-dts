package com.google.cloud.connector.api;

/**
 * An interface for collecting partitioned query information to prepare for parallel query
 * execution.
 */
public interface ParallelQueryPreparationContext {

  /**
   * Adds a partition for the parallel query execution.
   *
   * @param partitionData a byte array describing the partition. The same byte array will be
   *     provided to the {@link ParallelQueryExecutor#readPartition(AssetName, byte[])} method for
   *     reading in the data for the partition.
   */
  void addPartition(byte[] partitionData);

  /**
   * Sets the maximum number of concurrent calls for reading in data in parallel. By default, it
   * equals to the number of partitions being added through the {@link #addPartition(byte[])}
   * method.
   */
  void setMaxConcurrency(int maxConcurrency);
}
