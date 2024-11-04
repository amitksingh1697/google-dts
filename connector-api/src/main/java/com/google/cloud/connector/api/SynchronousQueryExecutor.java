package com.google.cloud.connector.api;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import java.io.IOException;

/** An interface for supporting synchronous query execution. */
public interface SynchronousQueryExecutor {

  /**
   * Executes a query over the given asset.
   *
   * @param assetName the name of the asset for the query to execute on
   * @param dataQuery the query to execute
   * @return a {@link RecordReader} for reading in the result of the query
   * @throws IOException if failed to execute the query
   */
  RecordReader execute(AssetName assetName, DataQuery dataQuery) throws IOException;
}
