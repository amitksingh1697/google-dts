package com.google.cloud.connector.schema;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.StructField;
import com.google.cloud.bigquery.federation.v1alpha1.Type;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import com.google.common.base.Preconditions;

/** A {@link SchemaBuilder} for creating an instance of {@link Schema}. */
public final class ConnectorSchemaBuilder implements SchemaBuilder {

  private final ConnectorStructSchemaBuilder<Void> delegate;
  private String name;
  private Type structType;

  public ConnectorSchemaBuilder() {
    this.delegate = new ConnectorStructSchemaBuilder<>(null, type -> structType = type);
  }

  @Override
  public SchemaBuilder name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns an instance of {@link Schema}.
   *
   * @return an instance of {@link Schema}
   */
  public Schema createSchema() {
    Preconditions.checkState(
        structType != null,
        "Schema was not built correctly. Make sure the method chain was called correctly with"
            + " endStruct() after every struct type.");

    Schema.Builder builder = Schema.newBuilder();
    if (name != null) {
      builder.setName(name);
    }

    return structType.getStructType().getFieldsList().stream()
        .map(ConnectorSchemaBuilder::toSchemaField)
        .collect(
            () -> builder,
            Schema.Builder::addFields,
            (left, right) -> {
              throw new IllegalArgumentException("Combining Schema.Builder is not supported");
            })
        .build();
  }

  @Override
  public SchemaTypeBuilder<StructSchemaBuilder<Void>> field(String name) {
    return delegate.field(name);
  }

  @Override
  public Void endStruct() {
    return delegate.endStruct();
  }

  private static Schema.Field toSchemaField(StructField field) {
    return Schema.Field.newBuilder()
        .setFieldName(field.getFieldName())
        .setType(field.getFieldType())
        .build();
  }
}
