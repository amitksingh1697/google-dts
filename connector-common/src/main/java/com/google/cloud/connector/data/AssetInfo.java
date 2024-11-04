package com.google.cloud.connector.data;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery.SourceCase;
import com.google.cloud.connector.api.AssetName;
import javax.annotation.Nullable;

/**
 * A record to store information of an asset, to be passed as a response in StartQuery.
 *
 * @param assetName of type {@link AssetName}
 * @param nativeQuery string containing the native query, if present
 */
public record AssetInfo(AssetName assetName, @Nullable String nativeQuery) {

  public SourceCase getSourceCase() {
    return assetName == AssetName.ROOT_ASSET ? SourceCase.NATIVE_QUERY : SourceCase.NAMED_TABLE;
  }
}
