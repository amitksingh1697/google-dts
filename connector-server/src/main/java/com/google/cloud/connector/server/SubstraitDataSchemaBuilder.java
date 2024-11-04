package com.google.cloud.connector.server;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.SubstraitSchema;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import com.google.cloud.connector.schema.SubstraitSchemaBuilder;

/**
 * A {@link DataSchemaBuilder} that uses {@link SubstraitSchemaBuilder} for building the {@link
 * Schema} object, which uses the {@link io.substrait.proto.NamedStruct} as the schema.
 */
public final class SubstraitDataSchemaBuilder implements DataSchemaBuilder {

  private final SubstraitSchemaBuilder delegate = new SubstraitSchemaBuilder();

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
    return Schema.newBuilder()
        .setSubstraitSchema(SubstraitSchema.newBuilder().setSchema(delegate.createNamedStruct()))
        .build();
  }
}
