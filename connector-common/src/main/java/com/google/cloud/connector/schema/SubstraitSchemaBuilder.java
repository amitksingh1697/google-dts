package com.google.cloud.connector.schema;

import com.google.cloud.connector.api.schema.SchemaBuilder;
import com.google.cloud.connector.api.schema.SchemaTypeBuilder;
import com.google.cloud.connector.api.schema.StructSchemaBuilder;
import com.google.common.base.Preconditions;
import io.substrait.proto.NamedStruct;
import io.substrait.proto.Type.Struct;
import java.util.ArrayList;
import java.util.List;

/** A {@link SchemaBuilder} for creating an instance of Substrait {@link NamedStruct}. */
public final class SubstraitSchemaBuilder implements SchemaBuilder {

  private final List<String> fieldNames;
  private final StructSchemaBuilder<Void> delegate;
  private Struct structType;

  /**
   * Default Constructor.
   */
  public SubstraitSchemaBuilder() {
    this.fieldNames = new ArrayList<>();
    this.delegate =
        new SubstraitStructSchemaBuilder<>(null, fieldNames::add, t -> structType = t.getStruct());
  }

  @Override
  public SchemaBuilder name(String name) {
    // no-op. Substrait schema don't have name
    return this;
  }

  @Override
  public SchemaTypeBuilder<StructSchemaBuilder<Void>> field(String name) {
    return delegate.field(name);
  }

  @Override
  public Void endStruct() {
    return delegate.endStruct();
  }

  /** Creates a {@link NamedStruct} representing the schema being setup through this build. */
  public NamedStruct createNamedStruct() {
    Preconditions.checkState(
        structType != null,
        "Schema was not built correctly. Make sure the method chain was called correctly with"
            + " endStruct() after every struct type.");

    return NamedStruct.newBuilder().addAllNames(fieldNames).setStruct(structType).build();
  }
}
