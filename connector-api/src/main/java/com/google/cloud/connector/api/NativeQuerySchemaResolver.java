package com.google.cloud.connector.api;

import com.google.cloud.connector.api.schema.SchemaBuilder;

/** Support for schema resolution for a native query. */
public interface NativeQuerySchemaResolver {

  /**
   * Resolves the schema of the output of the @{code nativeQuery}.
   *
   * @param assetName the name of the asset in whose context the query is to be executed
   */
  void resolveSchema(AssetName assetName, String nativeQuery, SchemaBuilder schemaBuilder);
}