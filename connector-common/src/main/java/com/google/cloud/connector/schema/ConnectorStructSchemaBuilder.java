package com.google.cloud.connector.schema;

import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.STRUCT;

import com.google.cloud.bigquery.federation.v1alpha1.StructField;
import com.google.cloud.bigquery.federation.v1alpha1.StructType;
import com.google.cloud.bigquery.federation.v1alpha1.Type;
import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import com.google.common.base.Preconditions;
import java.util.function.Consumer;

/** A {@link StructSchemaBuilder} for building {@link StructType}. */
final class ConnectorStructSchemaBuilder<T> implements StructSchemaBuilder<T> {

  private final T parentBuilder;
  private final Consumer<Type> typeConsumer;
  private final StructType.Builder structTypeBuilder;

  public ConnectorStructSchemaBuilder(T parentBuilder, Consumer<Type> typeConsumer) {
    this.parentBuilder = parentBuilder;
    this.typeConsumer = typeConsumer;
    this.structTypeBuilder = StructType.newBuilder();
  }

  @Override
  public SchemaTypeBuilder<StructSchemaBuilder<T>> field(String name) {
    return new ConnectorSchemaTypeBuilder<>(
        this,
        fieldType ->
            structTypeBuilder.addFields(
                StructField.newBuilder().setFieldName(name).setFieldType(fieldType)));
  }

  @Override
  public T endStruct() {
    Preconditions.checkState(
        structTypeBuilder.getFieldsCount() > 0,
        "A schema should have at least one field, but none is provided");
    typeConsumer.accept(
        Type.newBuilder().setTypeKind(STRUCT).setStructType(structTypeBuilder).build());
    return parentBuilder;
  }
}
