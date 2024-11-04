package com.google.cloud.connector.api.data;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.TypeKind;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Interface for setting data value based on the {@link Schema}.
 *
 * @param <T> The parent builder class.
 */
public interface ValueSetter<T> {

  /**
   * Sets with a {@link String} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#STRING}
   *   <li>{@link TypeKind#BOOL} (The boolean value is determined by {@link
   *       Boolean#valueOf(String)})
   *   <li>{@link TypeKind#NUMERIC} (Refer to {@link BigDecimal#BigDecimal(String)} for String to
   *       Decimal Conversion rules)
   *   <li>{@link TypeKind#BIGNUMERIC} (Refer to {@link BigDecimal#BigDecimal(String)} for String to
   *       Decimal Conversion rules)
   *   <li>{@link TypeKind#INT32}
   *   <li>{@link TypeKind#INT64}
   *   <li>{@link TypeKind#UINT32}
   *   <li>{@link TypeKind#UINT64}
   *   <li>{@link TypeKind#FLOAT}
   *   <li>{@link TypeKind#DOUBLE}
   *   <li>{@link TypeKind#JSON}
   * </ul>
   *
   * @param value the {@link String} to set.
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(String value);

  /**
   * Sets with a {@link BigDecimal} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#NUMERIC}
   *   <li>{@link TypeKind#BIGNUMERIC}
   *   <li>{@link TypeKind#STRING} (Refer to {@link BigDecimal#toPlainString()} for Decimal to
   *       String Conversion rules)
   * </ul>
   *
   * @param value the {@link BigDecimal} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(BigDecimal value);

  /**
   * Sets with a {@code boolean} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#BOOL}
   *   <li>{@link TypeKind#STRING} (converted to "true"/"false")
   * </ul>
   *
   * @param value the boolean to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(boolean value);

  /**
   * Sets with a {@code int} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#INT32}
   *   <li>{@link TypeKind#UINT32}
   *   <li>{@link TypeKind#INT64}
   *   <li>{@link TypeKind#UINT64}
   *   <li>{@link TypeKind#NUMERIC}
   *   <li>{@link TypeKind#BIGNUMERIC}
   *   <li>{@link TypeKind#STRING}
   * </ul>
   *
   * @param value the int to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(int value);

  /**
   * Sets with a {@code long} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#INT64}
   *   <li>{@link TypeKind#UINT64}
   *   <li>{@link TypeKind#NUMERIC}
   *   <li>{@link TypeKind#BIGNUMERIC}
   *   <li>{@link TypeKind#STRING}
   * </ul>
   *
   * @param value the long to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(long value);

  /**
   * Sets with a {@link BigInteger} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#INT64}
   *   <li>{@link TypeKind#UINT64}
   *   <li>{@link TypeKind#NUMERIC}
   *   <li>{@link TypeKind#BIGNUMERIC}
   *   <li>{@link TypeKind#STRING}
   * </ul>
   *
   * @param value the {@link BigInteger} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(BigInteger value);

  /**
   * Sets with a {@code float} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#FLOAT}
   *   <li>{@link TypeKind#DOUBLE}
   *   <li>{@link TypeKind#NUMERIC}
   *   <li>{@link TypeKind#BIGNUMERIC}
   *   <li>{@link TypeKind#STRING}
   * </ul>
   *
   * @param value the float to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(float value);

  /**
   * Sets with a {@code double} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#DOUBLE}
   *   <li>{@link TypeKind#NUMERIC}
   *   <li>{@link TypeKind#BIGNUMERIC}
   *   <li>{@link TypeKind#STRING}
   * </ul>
   *
   * @param value the double to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(double value);

  /**
   * Sets with a {@code byte[]} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#BYTES}
   * </ul>
   *
   * @param value the byte array to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(byte[] value);

  /**
   * Sets with a {@link ByteBuffer} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#BYTES}
   * </ul>
   *
   * @param value the {@link ByteBuffer} to set. The ByteBuffer will be consumed and its position
   *     will be advanced to the buffer's limit.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(ByteBuffer value);

  /**
   * Sets with a {@link LocalDate} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#DATE}
   *   <li>{@link TypeKind#STRING} (Using ISO 8601)
   * </ul>
   *
   * @param value the {@link LocalDate} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(LocalDate value);

  /**
   * Sets with a {@link LocalDateTime} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#DATETIME}
   *   <li>{@link TypeKind#TIMESTAMP}
   *   <li>{@link TypeKind#STRING} (Using ISO 8601)
   * </ul>
   *
   * <p>If the field schema is either {@link TypeKind#DATETIME} or {@link TypeKind#TIMESTAMP},
   * the zone offset will be determined by the system setting.
   *
   * @param value the {@link LocalDateTime} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(LocalDateTime value);

  /**
   * Sets with a {@link Timestamp} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#TIMESTAMP}
   *   <li>{@link TypeKind#STRING} (Using ISO 8601)
   * </ul>
   *
   * @param value the {@link Timestamp} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(Timestamp value);

  /**
   * Sets with a {@link ZonedDateTime} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#DATETIME}
   *   <li>{@link TypeKind#TIMESTAMP}
   *   <li>{@link TypeKind#STRING} (Using ISO 8601)
   * </ul>
   *
   * @param value the {@link ZonedDateTime} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(ZonedDateTime value);

  /**
   * Sets with a {@link OffsetDateTime} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#DATETIME}
   *   <li>{@link TypeKind#TIMESTAMP}
   *   <li>{@link TypeKind#STRING} (Using ISO 8601)
   * </ul>
   *
   * @param value the {@link OffsetDateTime} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(OffsetDateTime value);

  /**
   * Sets with a {@link LocalTime} value. The field schema can be one of:
   *
   * <ul>
   *   <li>{@link TypeKind#TIME}
   *   <li>{@link TypeKind#STRING} (Using ISO 8601)
   * </ul>
   *
   * @param value the {@link LocalTime} to set.
   * @return the parent builder
   * @throws IllegalArgumentException if the value is incompatible with the schema of this field.
   */
  T set(LocalTime value);

  /**
   * Sets a null value.
   *
   * @return the parent builder
   * @throws IllegalArgumentException if the field schema is not nullable.
   */
  T setNull();

  /**
   * Sets an array value. The field schema has to be of type {@link TypeKind#ARRAY}.
   *
   * @return an {@link ArrayBuilder} to create an Array.
   * @throws IllegalArgumentException if the schema of the field is not an array.
   */
  ArrayBuilder<T> array();

  /**
   * Sets a struct value. The field schema has to be of type {@link TypeKind#STRUCT}.
   *
   * @return a {@link StructBuilder} to create a Struct.
   * @throws IllegalArgumentException if the schema of the field is not an map.
   */
  StructBuilder<T> struct();
}
