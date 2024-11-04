package com.google.cloud.connector.api.schema;

/**
 * A builder for specify data type information in a schema.
 *
 * @param <T> type of the parent builder
 */
public interface SchemaTypeBuilder<T> {

  /**
   * Sets the field to be nullable.
   *
   * @return this builder
   */
  SchemaTypeBuilder<T> nullable();

  /**
   * Sets the field to be required.
   *
   * @return this builder
   */
  SchemaTypeBuilder<T> required();

  /**
   * Sets the field type to string.
   *
   * @return the parent builder
   */
  T typeString();

  /**
   * Sets the field type to integer.
   *
   * @return the parent builder
   */
  T typeUint64();

  /**
   * Sets the field type to integer.
   *
   * @return the parent builder
   */
  T typeUint32();

  /**
   * Sets the field type to integer.
   *
   * @return the parent builder
   */
  T typeInt64();

  /**
   * Sets the field type to integer.
   *
   * @return the parent builder
   */
  T typeInt32();

  /**
   * Sets the field type to float.
   *
   * @return the parent builder
   */
  T typeFloat();

  /**
   * Sets the field type to double.
   *
   * @return the parent builder
   */
  T typeDouble();

  /**
   * Sets the field type to boolean.
   *
   * @return the parent builder
   */
  T typeBool();

  /**
   * Sets the field type to date.
   *
   * @return the parent builder
   */
  T typeDate();

  /**
   * Sets the field type to datetime.
   *
   * @return the parent builder
   */
  T typeDateTime();

  /**
   * Sets the field type to bytes.
   *
   * @return the parent builder
   */
  T typeBytes();

  /**
   * Sets the field type to timestamp.
   *
   * @return the parent builder
   */
  T typeTimestamp();

  /**
   * Sets the field type to time.
   *
   * @return the parent builder
   */
  T typeTime();

  /**
   * Sets the field type to geography.
   *
   * @return the parent builder
   */
  T typeGeography();

  /**
   * Sets the field type to numeric.
   *
   * @return the parent builder
   */
  T typeNumeric();

  /**
   * Sets the field type to big numeric.
   *
   * @return the parent builder
   */
  T typeBigNumeric();

  /**
   * Sets the field type to interval.
   *
   * @return the parent builder
   */
  T typeInterval();

  /**
   * Sets the field type to json.
   *
   * @return the parent builder
   */
  T typeJson();

  /**
   * Sets the field type to array.
   *
   * @return an {@link SchemaTypeBuilder} for setting the type of array elements.
   */
  SchemaTypeBuilder<T> typeArray();

  /**
   * Sets the field type to struct.
   *
   * @return the {@link StructSchemaBuilder} for building the schema of the struct.
   */
  StructSchemaBuilder<T> typeStruct();

}
