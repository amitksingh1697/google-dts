package com.google.cloud.connector.schema;

import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import com.google.common.base.Preconditions;
import io.substrait.proto.Type;
import io.substrait.proto.Type.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A {@link StructSchemaBuilder} for building Substrait {@link Struct} type. */
final class SubstraitStructSchemaBuilder<T> implements StructSchemaBuilder<T> {

  private final T parentBuilder;
  private final Consumer<String> nameConsumer;
  private final Consumer<Type> typeConsumer;
  private final List<Type> fieldTypes;

  SubstraitStructSchemaBuilder(
      T parentBuilder, Consumer<String> nameConsumer, Consumer<Type> typeConsumer) {
    this.parentBuilder = parentBuilder;
    this.nameConsumer = nameConsumer;
    this.typeConsumer = typeConsumer;
    this.fieldTypes = new ArrayList<>();
  }

  @Override
  public SchemaTypeBuilder<StructSchemaBuilder<T>> field(String name) {
    nameConsumer.accept(name);
    return new SubstraitSchemaTypeBuilder<>(this, fieldTypes::add, nameConsumer);
  }

  @Override
  public T endStruct() {
    Preconditions.checkState(!fieldTypes.isEmpty(), "No fields was added for the struct ");
    typeConsumer.accept(
        Type.newBuilder().setStruct(Struct.newBuilder().addAllTypes(fieldTypes)).build());
    return parentBuilder;
  }
}
