package com.google.cloud.connector.data;

import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.ARRAY;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.BYTES;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.STRING;
import static com.google.cloud.bigquery.federation.v1alpha1.TypeKind.STRUCT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.ResolverStyle.STRICT;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import com.google.cloud.bigquery.federation.v1alpha1.ArrayValue;
import com.google.cloud.bigquery.federation.v1alpha1.StructField;
import com.google.cloud.bigquery.federation.v1alpha1.StructValue;
import com.google.cloud.bigquery.federation.v1alpha1.Type;
import com.google.cloud.bigquery.federation.v1alpha1.Value;
import com.google.cloud.connector.api.data.ArrayBuilder;
import com.google.cloud.connector.api.data.StructBuilder;
import com.google.cloud.connector.api.data.ValueSetter;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import com.google.protobuf.NullValue;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** A {@link ValueSetter} that produces {@link Value} as a result based on the data {@link Type}. */
class ConnectorValueSetter<T> implements ValueSetter<T> {

  private static final DateTimeFormatter LOCAL_TIME_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendValue(HOUR_OF_DAY, 2)
          .appendLiteral(':')
          .appendValue(MINUTE_OF_HOUR, 2)
          .optionalStart()
          .appendLiteral(':')
          .appendValue(SECOND_OF_MINUTE, 2)
          .appendFraction(MICRO_OF_SECOND, 6, 6, true)
          .toFormatter()
          .withResolverStyle(STRICT);

  private final T parentBuilder;
  private final String fieldPath;
  private final Type dataType;
  private final ZoneOffset dateTimeZoneOffset;
  private final Consumer<Value> valueConsumer;

  /**
   * Creates a new instance.
   *
   * @param parentBuilder the instance to return by each of the {@code set} method. The value is not
   *     used by this class besides just returning for chaining purpose.
   * @param fieldPath a dot separated string indicating the path to the field from the root of the
   *     structure for informative purpose.
   * @param dataType the schema {@link Type} for this value to be set
   * @param dateTimeZoneOffset the {@link ZoneOffset} for resolving {@link LocalDateTime} into a
   *     {@link ZonedDateTime}
   * @param valueConsumer a {@link Consumer} to receive the {@link Value} object after a value is
   *     being set.
   */
  ConnectorValueSetter(
      @Nullable T parentBuilder,
      String fieldPath,
      Type dataType,
      ZoneOffset dateTimeZoneOffset,
      Consumer<Value> valueConsumer) {
    this.parentBuilder = parentBuilder;
    this.fieldPath = fieldPath;
    this.dataType = dataType;
    this.dateTimeZoneOffset = dateTimeZoneOffset;
    this.valueConsumer = valueConsumer;
  }

  @Override
  public T set(String value) {
    switch (dataType.getTypeKind()) {
      case STRING -> acceptValue(Value.Builder::setStringValue, value);
      case INT32 -> acceptValue(Value.Builder::setInt32Value, Integer.parseInt(value));
      case INT64 -> acceptValue(Value.Builder::setInt64Value, Long.parseLong(value));
      case UINT32 -> acceptValue(Value.Builder::setInt32Value, Integer.parseInt(value));
      case UINT64 -> acceptValue(Value.Builder::setInt32Value, Integer.parseInt(value));
      case FLOAT -> acceptValue(Value.Builder::setFloatValue, Float.parseFloat(value));
      case DOUBLE -> acceptValue(Value.Builder::setDoubleValue, Double.parseDouble(value));
      case BOOL -> acceptValue(Value.Builder::setBoolValue, Boolean.parseBoolean(value));
      case NUMERIC, BIGNUMERIC -> set(new BigDecimal(value));
      case GEOGRAPHY -> acceptValue(Value.Builder::setGeographyValue, value);
      case INTERVAL -> acceptValue(Value.Builder::setIntervalValue, value);
      case JSON -> acceptValue(Value.Builder::setJsonValue, value);
      default -> throw throwIncompatibleType(String.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(BigDecimal value) {
    switch (dataType.getTypeKind()) {
      case NUMERIC -> {
        MathContext mc = new MathContext(38);
        acceptValue(
            Value.Builder::setNumericValue,
            value.round(mc).setScale(9, mc.getRoundingMode()).toPlainString());
      }
      case BIGNUMERIC -> {
        MathContext mc = new MathContext(77);
        acceptValue(
            Value.Builder::setBignumericValue,
            value.round(mc).setScale(38, mc.getRoundingMode()).toPlainString());
      }
      case STRING -> set(value.toPlainString());
      default -> throw throwIncompatibleType(String.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(boolean value) {
    switch (dataType.getTypeKind()) {
      case BOOL -> acceptValue(Value.Builder::setBoolValue, value);
      case STRING -> set(Boolean.toString(value));
      default -> throw throwIncompatibleType(boolean.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(int value) {
    switch (dataType.getTypeKind()) {
      case INT32 -> acceptValue(Value.Builder::setInt32Value, value);
      case INT64 -> acceptValue(Value.Builder::setInt64Value, (long) value);
      case UINT32 -> acceptValue(Value.Builder::setUint32Value, value);
      case UINT64 -> acceptValue(Value.Builder::setUint64Value, (long) value);
      case NUMERIC, BIGNUMERIC -> set(new BigDecimal(value));
      case STRING -> set(Integer.toString(value));
      default -> throw throwIncompatibleType(int.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(long value) {
    switch (dataType.getTypeKind()) {
      case INT64 -> acceptValue(Value.Builder::setInt64Value, value);
      case UINT64 -> acceptValue(Value.Builder::setUint64Value, value);
      case NUMERIC, BIGNUMERIC -> set(new BigDecimal(value));
      case STRING -> set(Long.toString(value));
      default -> throw throwIncompatibleType(long.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(BigInteger value) {
    switch (dataType.getTypeKind()) {
      case INT64 -> acceptValue(Value.Builder::setInt64Value, value.longValueExact());
      case UINT64 -> acceptValue(Value.Builder::setUint64Value, value.longValueExact());
      case NUMERIC, BIGNUMERIC -> set(new BigDecimal(value));
      case STRING -> set(value.toString());
      default -> throw throwIncompatibleType(BigInteger.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(float value) {
    switch (dataType.getTypeKind()) {
      case FLOAT -> acceptValue(Value.Builder::setFloatValue, value);
      case DOUBLE -> acceptValue(Value.Builder::setDoubleValue, (double) value);
      case NUMERIC, BIGNUMERIC -> set(new BigDecimal(value));
      case STRING -> set(Float.toString(value));
      default -> throw throwIncompatibleType(float.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(double value) {
    switch (dataType.getTypeKind()) {
      case DOUBLE -> acceptValue(Value.Builder::setDoubleValue, value);
      case NUMERIC, BIGNUMERIC -> set(new BigDecimal(value));
      case STRING -> set(Double.toString(value));
      default -> throw throwIncompatibleType(double.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(byte[] value) {
    if (dataType.getTypeKind() != BYTES) {
      throw throwIncompatibleType(byte[].class);
    }
    acceptValue(Value.Builder::setBytesValue, ByteString.copyFrom(value));
    return parentBuilder;
  }

  @Override
  public T set(ByteBuffer value) {
    if (dataType.getTypeKind() != BYTES) {
      throw throwIncompatibleType(ByteBuffer.class);
    }
    acceptValue(Value.Builder::setBytesValue, ByteString.copyFrom(value));
    return parentBuilder;
  }

  @Override
  public T set(LocalDate value) {
    switch (dataType.getTypeKind()) {
      case DATE -> acceptValue(Value.Builder::setDateValue, Ints.checkedCast(value.toEpochDay()));
      case STRING -> set(ISO_LOCAL_DATE.format(value));
      default -> throw throwIncompatibleType(LocalDate.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(LocalDateTime value) {
    if (dataType.getTypeKind() != STRING) {
      return set(value.atOffset(dateTimeZoneOffset), LocalDateTime.class);
    }
    return set(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value));
  }

  @Override
  public T set(Timestamp value) {
    switch (dataType.getTypeKind()) {
      case TIMESTAMP -> acceptValue(
          Value.Builder::setTimestampValue, toProtoTimestamp(value.toInstant()));
      case STRING -> set(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value.toLocalDateTime()));
      default -> throw throwIncompatibleType(Timestamp.class);
    }
    return parentBuilder;
  }

  @Override
  public T set(ZonedDateTime value) {
    return set(value.toOffsetDateTime(), ZonedDateTime.class);
  }

  @Override
  public T set(OffsetDateTime value) {
    return set(value, OffsetDateTime.class);
  }

  private T set(OffsetDateTime value, Class<?> requestClass) {
    switch (dataType.getTypeKind()) {
      case DATETIME -> acceptValue(Value.Builder::setDatetimeValue, toProtoDateTime(value));
      case TIMESTAMP -> acceptValue(
          Value.Builder::setTimestampValue, toProtoTimestamp(value.toInstant()));
      case STRING -> set(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
      default -> throw throwIncompatibleType(requestClass);
    }
    return parentBuilder;
  }

  @Override
  public T set(LocalTime value) {
    switch (dataType.getTypeKind()) {
      case TIME -> acceptValue(Value.Builder::setTimeValue, LOCAL_TIME_FORMATTER.format(value));
      case STRING -> set(LOCAL_TIME_FORMATTER.format(value));
      default -> throw throwIncompatibleType(LocalTime.class);
    }
    return parentBuilder;
  }

  @Override
  public T setNull() {
    acceptValue(Value.Builder::setNullValue, NullValue.NULL_VALUE);
    return parentBuilder;
  }

  @Override
  public ArrayBuilder<T> array() {
    Preconditions.checkArgument(
        dataType.getTypeKind() == ARRAY && dataType.hasArrayType(),
        "Data type for field '%s' of type '%s' is not an array",
        fieldPath,
        dataType.getTypeKind());

    Type elementType = dataType.getArrayType().getElementType();
    ArrayValue.Builder arrayBuilder = ArrayValue.newBuilder();

    return new ArrayBuilder<>() {
      @Override
      public ValueSetter<ArrayBuilder<T>> add() {
        return new ConnectorValueSetter<>(
            this, fieldPath + "[]", elementType, dateTimeZoneOffset, arrayBuilder::addElements);
      }

      @Override
      public T endArray() {
        acceptValue(Value.Builder::setArrayValue, arrayBuilder);
        return parentBuilder;
      }
    };
  }

  @Override
  public StructBuilder<T> struct() {
    Preconditions.checkArgument(
        dataType.getTypeKind() == STRUCT && dataType.hasStructType(),
        "Data type for field '%s' of type '%s' is not a struct",
        fieldPath,
        dataType.getTypeKind());

    Map<String, StructField> fields =
        dataType.getStructType().getFieldsList().stream()
            .collect(Collectors.toUnmodifiableMap(StructField::getFieldName, f -> f));
    Map<String, Value> fieldValues = new HashMap<>();

    return new StructBuilder<>() {
      @Override
      public ValueSetter<StructBuilder<T>> field(String fieldName) {
        StructField field = fields.get(fieldName);
        Preconditions.checkArgument(
            field != null, "Field '%s' is not defined in the struct at '%s'", fieldName, fieldPath);
        return new ConnectorValueSetter<>(
            this,
            fieldPath.isEmpty() ? fieldName : fieldPath + "." + fieldName,
            field.getFieldType(),
            dateTimeZoneOffset,
            v -> fieldValues.put(fieldName, v));
      }

      @Override
      public T endStruct() {
        acceptValue(
            Value.Builder::setStructValue,
            dataType.getStructType().getFieldsList().stream()
                .map(StructField::getFieldName)
                .map(
                    name ->
                        fieldValues.computeIfAbsent(
                            name,
                            k -> Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build()))
                .collect(
                    StructValue::newBuilder,
                    StructValue.Builder::addFields,
                    (left, right) -> {
                      throw new IllegalArgumentException(
                          "Combining StructValue.Builder is not supported");
                    }));
        return parentBuilder;
      }
    };
  }

  /**
   * Creates a proto {@link DateTime} from the given {@link OffsetDateTime}. The returned message is
   * intentionally not setting the time zone as per the types proto definition.
   *
   * @param offsetDateTime the {@link OffsetDateTime} to convert.
   */
  private DateTime toProtoDateTime(OffsetDateTime offsetDateTime) {
    OffsetDateTime odt = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);

    return DateTime.newBuilder()
        .setYear(odt.getYear())
        .setMonth(odt.getMonthValue())
        .setDay(odt.getDayOfMonth())
        .setHours(odt.getHour())
        .setMinutes(odt.getMinute())
        .setSeconds(odt.getSecond())
        .setNanos(odt.getNano())
        .build();
  }

  private com.google.protobuf.Timestamp toProtoTimestamp(Instant instant) {
    return com.google.protobuf.Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  private <V> void acceptValue(BiFunction<Value.Builder, V, Value.Builder> func, V value) {
    valueConsumer.accept(func.apply(Value.newBuilder(), value).build());
  }

  private IllegalArgumentException throwIncompatibleType(Class<?> type) {
    throw new IllegalArgumentException(
        String.format(
            "Value of '%s' type is incompatible for field '%s' of type '%s'",
            type.getName(), fieldPath, type));
  }
}
