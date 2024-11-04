package com.google.cloud.connector.data;

import com.google.cloud.bigquery.federation.v1alpha1.Type;
import com.google.cloud.bigquery.federation.v1alpha1.Value;
import com.google.cloud.connector.api.data.RecordBuilder;
import com.google.cloud.connector.api.data.StructBuilder;
import com.google.cloud.connector.api.data.ValueSetter;
import com.google.common.base.Preconditions;
import java.time.ZoneOffset;

/**
 * A {@link RecordBuilder} for creating {@link Value} representing a record produced by the
 * connector.
 */
public class ConnectorRecordBuilder implements RecordBuilder {

  private final StructBuilder<Void> delegate;
  private Value value;

  /**
   * Creates an instance.
   *
   * @param structType the schema of the record to be built by this builder;
   * @param dateTimeZoneOffset the {@link ZoneOffset} to apply on input of {@link
   *     java.time.LocalDateTime} type.
   */
  public ConnectorRecordBuilder(Type structType, ZoneOffset dateTimeZoneOffset) {
    this.delegate =
        new ConnectorValueSetter<Void>(null, "", structType, dateTimeZoneOffset, this::setValue)
            .struct();
  }

  @Override
  public ValueSetter<StructBuilder<Void>> field(String fieldName) {
    return delegate.field(fieldName);
  }

  @Override
  public Void endStruct() {
    return delegate.endStruct();
  }

  /** Returns the {@link Value} that was created by the last call to {@link #endStruct()}. */
  public Value getValue() {
    Preconditions.checkState(
        value != null, "The endStruct method must be called before getting the value");
    return value;
  }

  private void setValue(Value value) {
    this.value = value;
  }
}
