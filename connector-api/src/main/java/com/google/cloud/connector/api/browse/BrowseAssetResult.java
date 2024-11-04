package com.google.cloud.connector.api.browse;

import com.google.cloud.connector.api.BrowseAsset;

/** An iterator for child assets that are produced from browsing a parent asset. */
public interface BrowseAssetResult extends Iterable<BrowseAsset> {}
