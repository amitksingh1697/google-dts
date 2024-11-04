package com.google.cloud.connector.api.schema;

/**
 * A builder for creating a schema for a struct, which contains a collection of fields.
 *
 * @param <T> type of the parent builder
 */
public interface StructSchemaBuilder<T> {

  /**
   * Invoked to create a field for the schema.
   *
   * @param name the name for the field in a schema.
   * @return a {@link SchemaTypeBuilder} for specifying details (e.g. type) of the field.
   */
  SchemaTypeBuilder<StructSchemaBuilder<T>> field(String name);

  /**
   * Invoked to declare the end of the struct schema.
   *
   * @return the parent builder
   */
  T endStruct();
}
