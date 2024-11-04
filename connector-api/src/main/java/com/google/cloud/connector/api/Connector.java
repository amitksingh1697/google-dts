package com.google.cloud.connector.api;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.connector.api.schema.SchemaBuilder;

/** A baseline interface for all connectors to implement. */
public interface Connector {

  /**
   * Fetches the {@link Schema} of the asset identified by {@link AssetName}.
   *
   * @param assetName the name of the asset whose schema needs to be fetched.
   */
  void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder);
}
