package com.google.cloud.connector.server;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.connector.api.schema.SchemaBuilder;

/** A {@link SchemaBuilder} that expose the schema being built. */
public interface DataSchemaBuilder extends SchemaBuilder {

  /** Returns the {@link Schema} built by this {@link SchemaBuilder}. */
  Schema getSchema();
}
