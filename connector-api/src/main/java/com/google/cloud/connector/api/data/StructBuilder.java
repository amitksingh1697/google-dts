package com.google.cloud.connector.api.data;

/**
 * The interface to create a struct.
 *
 * @param <T> The parent builder.
 */
public interface StructBuilder<T> {

  /**
   * Invokes to set the value of a given field.
   *
   * @param fieldName name of the field to set. Multiple calls to this method with the same name
   *     will replace the previous call.
   * @return A {@link ValueSetter} to set the value of this field.
   */
  ValueSetter<StructBuilder<T>> field(String fieldName);

  /**
   * Invokes to finish building of the struct.
   *
   * @return the parent builder.
   */
  T endStruct();
}
