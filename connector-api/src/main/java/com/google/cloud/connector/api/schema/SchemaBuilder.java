package com.google.cloud.connector.api.schema;

/**
 * Interface for creating a schema.
 *
 * <p>Sample usage:
 *
 * <pre>{@code
 * // SchemaBuilder schemaBuilder = ...
 * schemaBuilder
 *     .name("schemaName")
 *     .field("firstName").required().typeString()
 *     .field("address").nullable().typeStruct()
 *         .field("street").typeString()
 *         .field("country").typeString()
 *         .endStruct()
 *     .field("age").typeInteger()
 *     .field("hobbies").typeArray().typeString()
 *     .endStruct();
 * }</pre>
 */
public interface SchemaBuilder extends StructSchemaBuilder<Void> {

  /**
   * Sets the name of the schema.
   *
   * @param name name of the schema
   * @return this {@link SchemaBuilder}
   */
  SchemaBuilder name(String name);
}
