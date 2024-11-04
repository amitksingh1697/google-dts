package com.google.cloud.connector.server;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import com.google.cloud.connector.schema.ConnectorSchemaBuilder;

/**
 * A {@link DataSchemaBuilder} that uses {@link ConnectorSchemaBuilder} for building the {@link
 * Schema} object.
 */
public final class ConnectorDataSchemaBuilder implements DataSchemaBuilder {

  private final ConnectorSchemaBuilder delegate = new ConnectorSchemaBuilder();

  @Override
  public SchemaBuilder name(String name) {
    return delegate.name(name);
  }

  @Override
  public SchemaTypeBuilder<StructSchemaBuilder<Void>> field(String name) {
    return delegate.field(name);
  }

  @Override
  public Void endStruct() {
    return delegate.endStruct();
  }

  @Override
  public Schema getSchema() {
    return delegate.createSchema();
  }
}
