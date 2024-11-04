package com.google.cloud.connector.data;

import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.ARRAY;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.BIGNUMERIC;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.BOOL;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.BYTES;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.DATE;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.DATETIME;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.DOUBLE;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.FLOAT;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.INT32;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.INT64;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.NUMERIC;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.STRING;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.STRUCT;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.TIME;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.TIMESTAMP;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.bigquery.federation.v1alpha1.ArrayType;
import com.google.cloud.bigquery.federation.v1alpha1.StructField;
import com.google.cloud.bigquery.federation.v1alpha1.StructType;
import com.google.cloud.bigquery.federation.v1alpha1.Type;
import com.google.cloud.bigquery.federation.v1alpha1.TypeKind;
import com.google.cloud.bigquery.federation.v1alpha1.Value;
import com.google.cloud.connector.api.data.ValueSetter;
import com.google.common.primitives.Ints;
import com.google.common.truth.Correspondence;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import com.google.type.DateTime;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Unit test for the {@link ConnectorValueSetter}. */
public class ConnectorValueSetterTest {

  private static final ZoneOffset DATE_TIME_ZONE_OFFSET = ZoneOffset.ofHours(-8);

  @Test
  public void set_string() {
    assertThat(createValueSetter(simpleType(STRING)).set("string").getValue())
        .isEqualTo(Value.newBuilder().setStringValue("string").build());
  }

  @Test
  public void set_booleanToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(true).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("true").build());
    assertThat(createValueSetter(simpleType(STRING)).set(false).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("false").build());
  }

  @Test
  public void set_intToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(10).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("10").build());
    assertThat(createValueSetter(simpleType(STRING)).set(-20).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("-20").build());
  }

  @Test
  public void set_longToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(30L).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("30").build());
    assertThat(createValueSetter(simpleType(STRING)).set(-40L).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("-40").build());
  }

  @Test
  public void set_bigDecimalToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(new BigDecimal("10e3")).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("10000").build());
  }

  @Test
  public void set_bigIntegerToString() {
    assertThat(
            createValueSetter(simpleType(STRING))
                .set(new BigInteger("98765432100000000000000"))
                .getValue())
        .isEqualTo(Value.newBuilder().setStringValue("98765432100000000000000").build());
  }

  @Test
  public void set_floatToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(3.14f).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("3.14").build());
  }

  @Test
  public void set_doubleToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(2.71828d).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("2.71828").build());
  }

  @Test
  public void set_localDateToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(LocalDate.of(2023, 1, 15)).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("2023-01-15").build());
  }

  @Test
  public void set_localDateTimeToString() {
    assertThat(
            createValueSetter(simpleType(STRING))
                .set(LocalDateTime.of(2023, 1, 15, 1, 22, 33, 45))
                .getValue())
        .isEqualTo(Value.newBuilder().setStringValue("2023-01-15T01:22:33.000000045").build());
  }

  @Test
  public void set_timestampToString() {
    assertThat(
            createValueSetter(simpleType(STRING))
                .set(Timestamp.valueOf("2023-01-20 11:22:05.033"))
                .getValue())
        .isEqualTo(Value.newBuilder().setStringValue("2023-01-20T11:22:05.033").build());
  }

  @Test
  public void set_zonedDateTimeToString() {
    assertThat(
            createValueSetter(simpleType(STRING))
                .set(ZonedDateTime.of(2022, 11, 22, 1, 2, 3, 0, ZoneId.of("UTC+5")))
                .getValue())
        .isEqualTo(Value.newBuilder().setStringValue("2022-11-22T01:02:03+05:00").build());
  }

  @Test
  public void set_offsetDateTimeToString() {
    assertThat(
            createValueSetter(simpleType(STRING))
                .set(OffsetDateTime.of(2023, 2, 1, 5, 6, 7, 0, ZoneOffset.ofHoursMinutes(11, 30)))
                .getValue())
        .isEqualTo(Value.newBuilder().setStringValue("2023-02-01T05:06:07+11:30").build());
  }

  @Test
  public void set_localTimeToString() {
    assertThat(createValueSetter(simpleType(STRING)).set(LocalTime.of(5, 6)).getValue())
        .isEqualTo(Value.newBuilder().setStringValue("05:06:00.000000").build());
  }

  @Test
  public void fail_binaryToString() {
    assertThrows(
        IllegalArgumentException.class,
        () -> createValueSetter(simpleType(STRING)).set(new byte[0]));
    assertThrows(
        IllegalArgumentException.class,
        () -> createValueSetter(simpleType(STRING)).set(ByteBuffer.wrap(new byte[0])));
  }

  @Test
  public void set_boolean() {
    assertTrue(createValueSetter(simpleType(BOOL)).set(true).getValue().getBoolValue());
    assertFalse(createValueSetter(simpleType(BOOL)).set(false).getValue().getBoolValue());
  }

  @Test
  public void set_stringToBoolean() {
    assertTrue(createValueSetter(simpleType(BOOL)).set("true").getValue().getBoolValue());
    assertFalse(createValueSetter(simpleType(BOOL)).set("false").getValue().getBoolValue());
  }

  @Test
  public void set_int() {
    assertThat(createValueSetter(simpleType(INT64)).set(30).getValue())
        .isEqualTo(Value.newBuilder().setInt64Value(30).build());
  }

  @Test
  public void set_bigIntegerToInt() {
    assertThat(createValueSetter(simpleType(INT64)).set(BigInteger.valueOf(100)).getValue())
        .isEqualTo(Value.newBuilder().setInt64Value(100).build());
  }

  @Test
  public void set_stringToInt() {
    assertThat(createValueSetter(simpleType(INT64)).set("15").getValue())
        .isEqualTo(Value.newBuilder().setInt64Value(15).build());
  }

  @Test
  public void fail_stringToInt() {
    assertThrows(
        NumberFormatException.class, () -> createValueSetter(simpleType(INT64)).set("15xy"));
  }

  @Test
  public void set_long() {
    assertThat(createValueSetter(simpleType(INT64)).set(120L).getValue())
        .isEqualTo(Value.newBuilder().setInt64Value(120L).build());
  }

  @Test
  public void set_intToLong() {
    assertThat(createValueSetter(simpleType(INT64)).set(Integer.MIN_VALUE).getValue())
        .isEqualTo(Value.newBuilder().setInt64Value(Integer.MIN_VALUE).build());
  }

  @Test
  public void set_bigIntegerToLong() {
    assertThat(createValueSetter(simpleType(INT64)).set(BigInteger.valueOf(100)).getValue())
        .isEqualTo(Value.newBuilder().setInt64Value(100).build());
  }

  @Test
  public void fail_bigIntegerToLong() {
    assertThrows(
        ArithmeticException.class,
        () ->
            createValueSetter(simpleType(INT64))
                .set(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN)));
  }

  @Test
  public void set_stringToLong() {
    assertThat(createValueSetter(simpleType(INT64)).set("9876543210").getValue())
        .isEqualTo(Value.newBuilder().setInt64Value(9876543210L).build());
  }

  @Test
  public void fail_stringToLong() {
    assertThrows(
        NumberFormatException.class, () -> createValueSetter(simpleType(INT64)).set("xyz"));
  }

  @Test
  public void set_float() {
    assertThat(createValueSetter(simpleType(FLOAT)).set(3.14f).getValue().getFloatValue())
        .isWithin(1.0e-4f)
        .of(3.14f);
  }

  @Test
  public void set_double() {
    assertThat(createValueSetter(simpleType(DOUBLE)).set(5.66541d).getValue().getDoubleValue())
        .isWithin(1.0e-10f)
        .of(5.66541d);
  }

  @Test
  public void set_floatToDouble() {
    assertThat(createValueSetter(simpleType(DOUBLE)).set(1.5d).getValue().getDoubleValue())
        .isWithin(1.0e-10f)
        .of(1.5d);
  }

  @Test
  public void set_stringToDouble() {
    assertThat(createValueSetter(simpleType(DOUBLE)).set("3.141592654").getValue().getDoubleValue())
        .isWithin(1.0e-10f)
        .of(3.141592654d);
  }

  @Test
  public void fail_stringToDouble() {
    assertThrows(
        NumberFormatException.class, () -> createValueSetter(simpleType(DOUBLE)).set("x.yz"));
  }

  @Test
  public void set_decimal() {
    assertThat(createValueSetter(simpleType(NUMERIC)).set(new BigDecimal("56.234")).getValue())
        .isEqualTo(Value.newBuilder().setNumericValue("56.234000000").build());
  }

  @Test
  public void set_decimalRounding() {
    assertThat(
            createValueSetter(simpleType(NUMERIC)).set(new BigDecimal("56.0123456789")).getValue())
        .isEqualTo(Value.newBuilder().setNumericValue("56.012345679").build());
  }

  @Test
  public void set_stringToDecimal() {
    assertThat(createValueSetter(simpleType(NUMERIC)).set("123.456").getValue())
        .isEqualTo(Value.newBuilder().setNumericValue("123.456000000").build());
  }

  @Test
  public void set_intToDecimal() {
    assertThat(createValueSetter(simpleType(NUMERIC)).set(20).getValue())
        .isEqualTo(Value.newBuilder().setNumericValue("20.000000000").build());
  }

  @Test
  public void set_longToDecimal() {
    assertThat(createValueSetter(simpleType(NUMERIC)).set(45678L).getValue())
        .isEqualTo(Value.newBuilder().setNumericValue("45678.000000000").build());
  }

  @Test
  public void set_floatToDecimal() {
    assertThat(createValueSetter(simpleType(NUMERIC)).set(1.618236542f).getValue())
        .isEqualTo(Value.newBuilder().setNumericValue("1.618236542").build());
  }

  @Test
  public void set_doubleToDecimal() {
    assertThat(createValueSetter(simpleType(NUMERIC)).set(7.142857142857d).getValue())
        .isEqualTo(Value.newBuilder().setNumericValue("7.142857143").build());
  }

  @Test
  public void set_bigIntegerToDecimal() {
    BigInteger bigInteger = new BigInteger(256, new Random());
    MathContext mc = new MathContext(38);
    BigDecimal expected = new BigDecimal(bigInteger, mc).setScale(9, mc.getRoundingMode());

    assertThat(createValueSetter(simpleType(NUMERIC)).set(bigInteger).getValue())
        .isEqualTo(Value.newBuilder().setNumericValue(expected.toPlainString()).build());
  }

  @Test
  public void set_stringToBigNumeric() {
    assertThat(createValueSetter(simpleType(BIGNUMERIC)).set("123.456").getValue())
        .isEqualTo(
            Value.newBuilder()
                .setBignumericValue("123.45600000000000000000000000000000000000")
                .build());
  }

  @Test
  public void set_intToBigNumeric() {
    assertThat(createValueSetter(simpleType(BIGNUMERIC)).set(20).getValue())
        .isEqualTo(
            Value.newBuilder()
                .setBignumericValue("20.00000000000000000000000000000000000000")
                .build());
  }

  @Test
  public void set_longToBigNumeric() {
    assertThat(createValueSetter(simpleType(BIGNUMERIC)).set(45678L).getValue())
        .isEqualTo(
            Value.newBuilder()
                .setBignumericValue("45678.00000000000000000000000000000000000000")
                .build());
  }

  @Test
  public void set_floatToBigNumeric() {
    assertThat(createValueSetter(simpleType(BIGNUMERIC)).set(1.25f).getValue())
        .isEqualTo(
            Value.newBuilder()
                .setBignumericValue("1.25000000000000000000000000000000000000")
                .build());
  }

  @Test
  public void set_doubleToBigNumeric() {
    assertThat(createValueSetter(simpleType(BIGNUMERIC)).set(128e18d).getValue())
        .isEqualTo(
            Value.newBuilder()
                .setBignumericValue("128000000000000000000.00000000000000000000000000000000000000")
                .build());
  }

  @Test
  public void set_bigIntegerToBigNumeric() {
    BigInteger bigInteger = new BigInteger(300, new Random()).negate();
    MathContext mc = new MathContext(77);
    BigDecimal expected = new BigDecimal(bigInteger, mc).setScale(38, mc.getRoundingMode());

    assertThat(createValueSetter(simpleType(BIGNUMERIC)).set(bigInteger).getValue())
        .isEqualTo(Value.newBuilder().setBignumericValue(expected.toPlainString()).build());
  }

  @Test
  public void set_bigDecimalToBigNumeric() {
    assertThat(
            createValueSetter(simpleType(BIGNUMERIC))
                .set(new BigDecimal("1234567890.12"))
                .getValue())
        .isEqualTo(
            Value.newBuilder()
                .setBignumericValue("1234567890.12000000000000000000000000000000000000")
                .build());
  }

  @Test
  public void set_binary() {
    assertThat(createValueSetter(simpleType(BYTES)).set(new byte[] {1, 2, 3}).getValue())
        .isEqualTo(
            Value.newBuilder().setBytesValue(ByteString.copyFrom(new byte[] {1, 2, 3})).build());

    assertThat(createValueSetter(simpleType(BYTES)).set(UTF_8.encode("test")).getValue())
        .isEqualTo(Value.newBuilder().setBytesValue(ByteString.copyFrom("test", UTF_8)).build());
  }

  @Test
  public void set_localDate() {
    assertThat(createValueSetter(simpleType(DATE)).set(LocalDate.of(1970, 1, 2)).getValue())
        .isEqualTo(Value.newBuilder().setDateValue(1).build());
    assertThat(createValueSetter(simpleType(DATE)).set(LocalDate.of(1969, 12, 30)).getValue())
        .isEqualTo(Value.newBuilder().setDateValue(-2).build());
  }

  @Test
  public void set_localTime() {
    assertThat(createValueSetter(simpleType(TIME)).set(LocalTime.of(20, 30, 40, 543210)).getValue())
        .isEqualTo(Value.newBuilder().setTimeValue("20:30:40.000543").build());
  }

  @Test
  public void set_timestamp() {
    Timestamp timestamp = Timestamp.valueOf("1980-1-13 01:02:03.123");
    com.google.protobuf.Timestamp expected = toProtoTimestamp(timestamp.toInstant());

    assertThat(createValueSetter(simpleType(TIMESTAMP)).set(timestamp).getValue())
        .isEqualTo(Value.newBuilder().setTimestampValue(expected).build());
  }

  @Test
  public void set_localDateTimeToTimestamp() {
    LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 20, 2, 0, 30);
    com.google.protobuf.Timestamp expected =
        toProtoTimestamp(localDateTime.toInstant(DATE_TIME_ZONE_OFFSET));

    assertThat(createValueSetter(simpleType(TIMESTAMP)).set(localDateTime).getValue())
        .isEqualTo(Value.newBuilder().setTimestampValue(expected).build());
  }

  @Test
  public void set_localDateTimeToDateTime() {
    LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 20, 2, 0, 30);
    OffsetDateTime expected =
        localDateTime.atOffset(DATE_TIME_ZONE_OFFSET).withOffsetSameInstant(ZoneOffset.UTC);

    Value value = createValueSetter(simpleType(DATETIME)).set(localDateTime).getValue();
    assertTrue(value.hasDatetimeValue());
    assertThat(fromProtoDateTime(value.getDatetimeValue()))
        .isEquivalentAccordingToCompareTo(expected);
  }

  @Test
  public void set_zonedDateTimeToTimestamp() {
    ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 2, 2, 7, 8, 9, 0, ZoneId.of("UTC+6"));

    assertThat(createValueSetter(simpleType(TIMESTAMP)).set(zonedDateTime).getValue())
        .isEqualTo(
            Value.newBuilder()
                .setTimestampValue(toProtoTimestamp(zonedDateTime.toInstant()))
                .build());
  }

  @Test
  public void set_zonedDateTimeToDateTime() {
    ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 2, 2, 7, 8, 9, 0, ZoneId.of("UTC+6"));
    OffsetDateTime expected =
        zonedDateTime.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);

    Value value = createValueSetter(simpleType(DATETIME)).set(zonedDateTime).getValue();
    assertTrue(value.hasDatetimeValue());
    assertThat(fromProtoDateTime(value.getDatetimeValue()))
        .isEquivalentAccordingToCompareTo(expected);
  }

  @Test
  public void set_offsetDateTimeToTimestamp() {
    OffsetDateTime offsetDateTime =
        OffsetDateTime.of(2023, 2, 2, 7, 8, 9, 0, ZoneOffset.ofHoursMinutes(5, 30));

    assertThat(createValueSetter(simpleType(TIMESTAMP)).set(offsetDateTime).getValue())
        .isEqualTo(
            Value.newBuilder()
                .setTimestampValue(toProtoTimestamp(offsetDateTime.toInstant()))
                .build());
  }

  @Test
  public void set_offsetDateTimeToDateTime() {
    OffsetDateTime offsetDateTime =
        OffsetDateTime.of(2023, 2, 2, 7, 8, 9, 0, ZoneOffset.ofHoursMinutes(5, 30));
    OffsetDateTime expected = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);

    Value value = createValueSetter(simpleType(DATETIME)).set(offsetDateTime).getValue();
    assertTrue(value.hasDatetimeValue());
    assertThat(fromProtoDateTime(value.getDatetimeValue()))
        .isEquivalentAccordingToCompareTo(expected);
  }

  @Test
  public void build_array() {
    assertThat(
            createValueSetter(arrayType(simpleType(INT64)))
                .array()
                .add()
                .set(1)
                .add()
                .set(2)
                .add()
                .set(3)
                .endArray()
                .getValue()
                .getArrayValue()
                .getElementsList())
        .comparingElementsUsing(
            Correspondence.transforming(Value::getInt64Value, "Value.getInt64Value"))
        .containsExactly(1L, 2L, 3L);
  }

  @Test
  public void fail_arrayPlainSet() {
    assertThrows(
        IllegalArgumentException.class,
        () -> createValueSetter(arrayType(simpleType(STRING))).set(1));
  }

  @Test
  public void build_struct() throws ParseException {
    Type structType =
        structType(
            structField("int", simpleType(INT64)),
            structField("string", simpleType(STRING)),
            structField("empty", simpleType(DATE)),
            structField("array", arrayType(simpleType(BOOL))));

    Value expected =
        TextFormat.parse(
            """
            struct_value {
              fields {
                int64_value: 1
              }
              fields {
                string_value: "2"
              }
              fields {
                null_value: NULL_VALUE
              }
              fields {
                array_value {
                  elements {
                    bool_value: true
                  }
                  elements {
                    bool_value: false
                  }
                }
              }
            }
            """,
            Value.class);

    ConnectorRecordBuilder recordBuilder =
        new ConnectorRecordBuilder(structType, DATE_TIME_ZONE_OFFSET);
    recordBuilder
        .field("int")
        .set(1)
        .field("string")
        .set(2)
        .field("array")
        .array()
        .add()
        .set(true)
        .add()
        .set(false)
        .endArray()
        .endStruct();
    assertThat(recordBuilder.getValue()).isEqualTo(expected);
  }

  @Test
  public void build_multipleRecords() {
    Type structType = structType(structField("int", simpleType(INT64)));

    ConnectorRecordBuilder builder = new ConnectorRecordBuilder(structType, DATE_TIME_ZONE_OFFSET);

    List<Value> values =
        IntStream.range(0, 5)
            .mapToObj(
                i -> {
                  builder.field("int").set(i).endStruct();
                  return builder.getValue();
                })
            .toList();

    assertThat(values)
        .comparingElementsUsing(
            Correspondence.<Value, Long>transforming(
                input -> input.getStructValue().getFields(0).getInt64Value(), "Get int field"))
        .containsExactly(0L, 1L, 2L, 3L, 4L);
  }

  @Test
  public void fail_nonExistsField() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ConnectorRecordBuilder(
                    structType(
                        structField("int", simpleType(INT64)),
                        structField("string", simpleType(STRING))),
                    DATE_TIME_ZONE_OFFSET)
                .field("long")
                .set(1L)
                .endStruct());
  }

  private static OffsetDateTime fromProtoDateTime(DateTime dateTime) {
    LocalDateTime localDateTime =
        LocalDateTime.of(
            dateTime.getYear(),
            dateTime.getMonth(),
            dateTime.getDay(),
            dateTime.getHours(),
            dateTime.getMinutes(),
            dateTime.getSeconds(),
            dateTime.getNanos());

    if (dateTime.hasUtcOffset()) {
      return localDateTime.atOffset(
          ZoneOffset.ofTotalSeconds(Ints.checkedCast(dateTime.getUtcOffset().getSeconds())));
    }
    if (dateTime.hasTimeZone()) {
      return localDateTime.atZone(ZoneId.of(dateTime.getTimeZone().getId())).toOffsetDateTime();
    }
    return localDateTime.atOffset(ZoneOffset.UTC);
  }

  private static com.google.protobuf.Timestamp toProtoTimestamp(Instant instant) {
    return com.google.protobuf.Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  private ValueSetter<ValueConsumer> createValueSetter(Type dataType) {
    ValueConsumer consumer = new ValueConsumer();
    return new ConnectorValueSetter<>(consumer, "", dataType, DATE_TIME_ZONE_OFFSET, consumer);
  }

  private Type simpleType(TypeKind typeKind) {
    return Type.newBuilder().setTypeKind(typeKind).build();
  }

  private Type arrayType(Type elementType) {
    return Type.newBuilder()
        .setTypeKind(ARRAY)
        .setArrayType(ArrayType.newBuilder().setElementType(elementType))
        .build();
  }

  private Type structType(StructField... fields) {
    return Type.newBuilder()
        .setTypeKind(STRUCT)
        .setStructType(StructType.newBuilder().addAllFields(Arrays.asList(fields)))
        .build();
  }

  private StructField structField(String name, Type type) {
    return StructField.newBuilder().setFieldName(name).setFieldType(type).build();
  }

  private static final class ValueConsumer implements Consumer<Value> {

    private Value value;

    @Override
    public void accept(Value value) {
      this.value = value;
    }

    Value getValue() {
      return value;
    }
  }
}
