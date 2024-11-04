package com.google.cloud.connector.api;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import java.io.IOException;

/** An interface for support parallel query execution. */
public interface ParallelQueryExecutor {

  /**
   * Prepares for the parallel query execution on the given asset. It is expected the {@link
   * ParallelQueryPreparationContext#addPartition(byte[])} method will be called from this method to
   * add one or more partitions for querying at the parallel query execution phase.
   *
   * @param assetName the name of the asset for the query to execute on
   * @param query the query to execute
   * @param context for setting up parallel execution information, such as partitioning
   */
  void prepareQuery(AssetName assetName, DataQuery query, ParallelQueryPreparationContext context);

  /**
   * Reads the data of the given partition.
   *
   * @param assetName the name of the asset for the original query to execute on
   * @param partitionData the partition data added through the {@link
   *     ParallelQueryPreparationContext#addPartition(byte[])} method
   * @return a {@link RecordReader} for reading in the data of the partition
   * @throws IOException if failed to read data from the given partition
   */
  RecordReader readPartition(AssetName assetName, byte[] partitionData) throws IOException;
}
