package com.google.cloud.connector.schema;

import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import io.substrait.proto.Type;
import io.substrait.proto.Type.Binary;
import io.substrait.proto.Type.Boolean;
import io.substrait.proto.Type.Date;
import io.substrait.proto.Type.Decimal;
import io.substrait.proto.Type.FP32;
import io.substrait.proto.Type.FP64;
import io.substrait.proto.Type.I32;
import io.substrait.proto.Type.I64;
import io.substrait.proto.Type.List;
import io.substrait.proto.Type.Nullability;
import io.substrait.proto.Type.Struct;
import io.substrait.proto.Type.Time;
import io.substrait.proto.Type.Timestamp;
import io.substrait.proto.Type.TimestampTZ;
import java.util.function.Consumer;

/** A {@link SchemaTypeBuilder} for building Substrait {@link Type}. */
final class SubstraitSchemaTypeBuilder<T> implements SchemaTypeBuilder<T> {

  private final T parentBuilder;
  private final Consumer<Type> typeConsumer;
  private final Consumer<String> nameConsumer;
  private Nullability nullability;

  SubstraitSchemaTypeBuilder(
      T parentBuilder, Consumer<Type> typeConsumer, Consumer<String> nameConsumer) {
    this.parentBuilder = parentBuilder;
    this.typeConsumer = typeConsumer;
    this.nameConsumer = nameConsumer;
    this.nullability = Nullability.NULLABILITY_UNSPECIFIED;
  }

  @Override
  public SchemaTypeBuilder<T> nullable() {
    nullability = Nullability.NULLABILITY_NULLABLE;
    return this;
  }

  @Override
  public SchemaTypeBuilder<T> required() {
    nullability = Nullability.NULLABILITY_REQUIRED;
    return this;
  }

  @Override
  public T typeString() {
    typeConsumer.accept(
        Type.newBuilder().setString(Type.String.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeInt32() {
    typeConsumer.accept(
        Type.newBuilder().setI32(I32.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeInt64() {
    typeConsumer.accept(
        Type.newBuilder().setI64(I64.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeUint32() {
    typeConsumer.accept(
        Type.newBuilder().setI32(I32.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeUint64() {
    typeConsumer.accept(
        Type.newBuilder().setI64(I64.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeFloat() {
    typeConsumer.accept(
        Type.newBuilder().setFp32(FP32.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeDouble() {
    typeConsumer.accept(
        Type.newBuilder().setFp64(FP64.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeBool() {
    typeConsumer.accept(
        Type.newBuilder().setBool(Boolean.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeDate() {
    typeConsumer.accept(
        Type.newBuilder().setDate(Date.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeDateTime() {
    typeConsumer.accept(
        Type.newBuilder().setTimestamp(Timestamp.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeBytes() {
    typeConsumer.accept(
        Type.newBuilder().setBinary(Binary.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeTimestamp() {
    typeConsumer.accept(
        Type.newBuilder()
            .setTimestampTz(TimestampTZ.newBuilder().setNullability(nullability))
            .build());
    return parentBuilder;
  }

  @Override
  public T typeTime() {
    typeConsumer.accept(
        Type.newBuilder().setTime(Time.newBuilder().setNullability(nullability)).build());
    return parentBuilder;
  }

  @Override
  public T typeGeography() {
    throw new UnsupportedOperationException();
  }

  @Override
  public T typeNumeric() {
    typeConsumer.accept(
        Type.newBuilder()
            .setDecimal(
                Decimal.newBuilder()
                    .setPrecision(38)
                    .setScale(9)
                    .setNullability(nullability)
                    .build())
            .build());
    return parentBuilder;
  }

  @Override
  public T typeBigNumeric() {
    throw new UnsupportedOperationException();
  }

  @Override
  public T typeInterval() {
    throw new UnsupportedOperationException();
  }

  @Override
  public T typeJson() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SchemaTypeBuilder<T> typeArray() {
    return new SubstraitSchemaTypeBuilder<>(
        parentBuilder,
        elementType ->
            typeConsumer.accept(
                Type.newBuilder()
                    .setList(List.newBuilder().setType(elementType).setNullability(nullability))
                    .build()),
        nameConsumer);
  }

  @Override
  public StructSchemaBuilder<T> typeStruct() {
    Struct nullabilityStruct = Struct.newBuilder().setNullability(nullability).build();
    return new SubstraitStructSchemaBuilder<>(
        parentBuilder,
        nameConsumer,
        type -> typeConsumer.accept(type.toBuilder().mergeStruct(nullabilityStruct).build()));
  }
}
