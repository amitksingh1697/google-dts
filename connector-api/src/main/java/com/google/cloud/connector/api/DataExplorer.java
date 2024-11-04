package com.google.cloud.connector.api;

import com.google.cloud.connector.api.browse.BrowseAssetResult;

/** Interface for data explore functionalities. */
public interface DataExplorer {
  /**
   * Returns a {@link BrowseAssetResult} that contains child assets produced from browsing the
   * specified parent asset.
   *
   * @param parentAssetName the name of the parent asset to browse.
   * @return a {@link BrowseAssetResult} that contains child assets of the given parent.
   */
  BrowseAssetResult browseAsset(AssetName parentAssetName);
}
