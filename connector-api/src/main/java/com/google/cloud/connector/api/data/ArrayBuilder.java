package com.google.cloud.connector.api.data;

/**
 * The interface to create an array.
 *
 * @param <T> The parent builder.
 */
public interface ArrayBuilder<T> {

  /**
   * Invokes to add a new element to the array.
   *
   * @return A {@link ValueSetter} to set the value of this new element.
   */
  ValueSetter<ArrayBuilder<T>> add();

  /**
   * Invokes to finish building of the array.
   *
   * @return the parent builder.
   */
  T endArray();
}
