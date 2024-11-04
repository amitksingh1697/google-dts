/**
 * Provides a common interface to create a record for plugins.
 *
 * <p>This interface contains:
 *
 * <ul>
 *   <li>{@link com.google.cloud.connector.api.data.ArrayBuilder} to create arrays
 *   <li>{@link com.google.cloud.connector.api.data.StructBuilder} to create structs
 *   <li>{@link com.google.cloud.connector.api.data.RecordBuilder} to create top level records</li>
 * </ul>
 *
 * <p>These classes then provide methods to obtain {@link
 * com.google.cloud.connector.api.data.ValueSetter} to set values.
 *
 * <p>Example use:
 *
 * <pre>
 * {@literal
 *     recordBuilder
 *       .field("f1").set(1)
 *       .field("f2").set("string")
 *       .field("array").array()
 *         .add().set(1)
 *         .add().set(2)
 *         .endArray()
 *       .field("struct").struct()
 *         .field("nested1").set(1)
 *         .field("deepstruct").struct()
 *           .field("deepnested").array()
 *             .add().set("1")
 *           .endArray()
 *         .endStruct()
 *       .endStruct();
 *       }
 * </pre>
 */
package com.google.cloud.connector.api.data;
