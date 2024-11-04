package com.google.cloud.connector.schema;

import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.ARRAY;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.BIGNUMERIC;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.BOOL;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.BYTES;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.DATE;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.DATETIME;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.DOUBLE;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.FLOAT;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.GEOGRAPHY;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.INT32;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.INT64;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.INTERVAL;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.JSON;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.NUMERIC;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.STRING;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.TIME;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.TIMESTAMP;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.UINT32;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.UINT64;

import com.google.cloud.bigquery.federation.v1alpha1.ArrayType;
import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.Type;
import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import java.util.function.Consumer;

/** A {@link SchemaTypeBuilder} for building {@link Schema} {@link Type}. */
final class ConnectorSchemaTypeBuilder<T> implements SchemaTypeBuilder<T> {

  private final T parentBuilder;
  private final Consumer<Type> typeConsumer;

  /** Creates an instance of field builder for a field in the parent schema. */
  ConnectorSchemaTypeBuilder(T parentBuilder, Consumer<Type> typeConsumer) {
    this.parentBuilder = parentBuilder;
    this.typeConsumer = typeConsumer;
  }

  @Override
  public SchemaTypeBuilder<T> nullable() {
    // no-op since the Schema type doesn't have a notion of nullable
    return this;
  }

  @Override
  public SchemaTypeBuilder<T> required() {
    // no-op since the Schema type doesn't have a notion of required
    return this;
  }

  @Override
  public T typeString() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(STRING).build());
    return parentBuilder;
  }

  @Override
  public T typeInt32() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(INT32).build());
    return parentBuilder;
  }

  @Override
  public T typeInt64() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(INT64).build());
    return parentBuilder;
  }

  @Override
  public T typeUint32() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(UINT32).build());
    return parentBuilder;
  }

  @Override
  public T typeUint64() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(UINT64).build());
    return parentBuilder;
  }


  @Override
  public T typeFloat() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(FLOAT).build());
    return parentBuilder;
  }

  @Override
  public T typeDouble() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(DOUBLE).build());
    return parentBuilder;
  }

  @Override
  public T typeBool() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(BOOL).build());
    return parentBuilder;
  }

  @Override
  public T typeDate() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(DATE).build());
    return parentBuilder;
  }

  @Override
  public T typeDateTime() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(DATETIME).build());
    return parentBuilder;
  }

  @Override
  public T typeBytes() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(BYTES).build());
    return parentBuilder;
  }

  @Override
  public T typeTimestamp() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(TIMESTAMP).build());
    return parentBuilder;
  }

  @Override
  public T typeTime() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(TIME).build());
    return parentBuilder;
  }

  @Override
  public T typeGeography() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(GEOGRAPHY).build());
    return parentBuilder;
  }

  @Override
  public T typeNumeric() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(NUMERIC).build());
    return parentBuilder;
  }

  @Override
  public T typeBigNumeric() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(BIGNUMERIC).build());
    return parentBuilder;
  }

  @Override
  public T typeInterval() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(INTERVAL).build());
    return parentBuilder;
  }

  @Override
  public T typeJson() {
    typeConsumer.accept(Type.newBuilder().setTypeKind(JSON).build());
    return parentBuilder;
  }

  @Override
  public SchemaTypeBuilder<T> typeArray() {
    return new ConnectorSchemaTypeBuilder<>(
        parentBuilder,
        elementType ->
            typeConsumer.accept(
                Type.newBuilder()
                    .setTypeKind(ARRAY)
                    .setArrayType(ArrayType.newBuilder().setElementType(elementType))
                    .build()));
  }

  @Override
  public StructSchemaBuilder<T> typeStruct() {
    return new ConnectorStructSchemaBuilder<>(parentBuilder, typeConsumer);
  }
}
