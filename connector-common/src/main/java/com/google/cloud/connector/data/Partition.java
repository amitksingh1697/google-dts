package com.google.cloud.connector.data;

import javax.annotation.Nullable;

/**
 * Record for storing information regarding a single partition of the StartQuery response.
 *
 * @param query byte array representing the SQL query
 * @param assetInfo of type {@link AssetInfo}
 */
public record Partition(@Nullable byte[] query, @Nullable AssetInfo assetInfo) {

  public boolean isEmpty() {
    return query == null;
  }
}
