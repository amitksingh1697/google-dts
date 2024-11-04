package com.google.cloud.connector.schema;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import io.substrait.proto.NamedStruct;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link SubstraitSchemaBuilder}. */
public class SubstraitSchemaBuilderTest {

  @Test
  public void buildNamedStruct_simpleTypes() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("string_type")
        .typeString()
        .field("int64_type")
        .typeInt64()
        .field("double_type")
        .typeDouble()
        .field("bool_type")
        .typeBool()
        .field("date_type")
        .typeDate()
        .field("datetime_type")
        .typeDateTime()
        .field("bytes_type")
        .typeBytes()
        .field("timestamp_type")
        .typeTimestamp()
        .field("time_type")
        .typeTime()
        .field("numeric_type")
        .typeNumeric()
        .endStruct();
    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "string_type"
              names: "int64_type"
              names: "double_type"
              names: "bool_type"
              names: "date_type"
              names: "datetime_type"
              names: "bytes_type"
              names: "timestamp_type"
              names: "time_type"
              names: "numeric_type"
              struct {
                types {
                  string {
                  }
                }
                types {
                  i64 {
                  }
                }
                types {
                  fp64 {
                  }
                }
                types {
                  bool {
                  }
                }
                types {
                  date {
                  }
                }
                types {
                  timestamp {
                  }
                }
                types {
                  binary {
                  }
                }
                types {
                  timestamp_tz {
                  }
                }
                types {
                  time {
                  }
                }
                types {
                  decimal {
                    scale: 9
                    precision: 38
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_nullable_simpleTypes() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("string_type")
        .nullable()
        .typeString()
        .field("int64_type")
        .nullable()
        .typeInt64()
        .field("double_type")
        .nullable()
        .typeDouble()
        .field("bool_type")
        .nullable()
        .typeBool()
        .field("date_type")
        .nullable()
        .typeDate()
        .field("datetime_type")
        .nullable()
        .typeDateTime()
        .field("bytes_type")
        .nullable()
        .typeBytes()
        .field("timestamp_type")
        .nullable()
        .typeTimestamp()
        .field("time_type")
        .nullable()
        .typeTime()
        .field("numeric_type")
        .nullable()
        .typeNumeric()
        .endStruct();
    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "string_type"
              names: "int64_type"
              names: "double_type"
              names: "bool_type"
              names: "date_type"
              names: "datetime_type"
              names: "bytes_type"
              names: "timestamp_type"
              names: "time_type"
              names: "numeric_type"
              struct {
                types {
                  string {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  i64 {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  fp64 {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  bool {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  date {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  timestamp {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  binary {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  timestamp_tz {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  time {
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  decimal {
                    nullability: NULLABILITY_NULLABLE
                    scale: 9
                    precision: 38
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_required_simpleTypes() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("string_type")
        .required()
        .typeString()
        .field("int64_type")
        .required()
        .typeInt64()
        .field("double_type")
        .required()
        .typeDouble()
        .field("bool_type")
        .required()
        .typeBool()
        .field("date_type")
        .required()
        .typeDate()
        .field("datetime_type")
        .required()
        .typeDateTime()
        .field("bytes_type")
        .required()
        .typeBytes()
        .field("timestamp_type")
        .required()
        .typeTimestamp()
        .field("time_type")
        .required()
        .typeTime()
        .field("numeric_type")
        .required()
        .typeNumeric()
        .endStruct();
    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "string_type"
              names: "int64_type"
              names: "double_type"
              names: "bool_type"
              names: "date_type"
              names: "datetime_type"
              names: "bytes_type"
              names: "timestamp_type"
              names: "time_type"
              names: "numeric_type"
              struct {
                types {
                  string {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  i64 {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  fp64 {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  bool {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  date {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  timestamp {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  binary {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  timestamp_tz {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  time {
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  decimal {
                    nullability: NULLABILITY_REQUIRED
                    scale: 9
                    precision: 38
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_arrayType() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("arrayBoolean")
        .typeArray()
        .typeBool()
        .field("arrayArray")
        .typeArray()
        .typeArray()
        .typeInt64()
        .field("arrayStruct")
        .typeArray()
        .typeStruct()
        .field("string")
        .typeString()
        .field("numeric")
        .typeNumeric()
        .endStruct()
        .endStruct();

    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "arrayBoolean"
              names: "arrayArray"
              names: "arrayStruct"
              names: "string"
              names: "numeric"
              struct {
                types {
                  list {
                    type {
                      bool {
                      }
                    }
                  }
                }
                types {
                  list {
                    type {
                      list {
                        type {
                          i64 {
                          }
                        }
                      }
                    }
                  }
                }
                types {
                  list {
                    type {
                      struct {
                        types {
                          string {
                          }
                        }
                        types {
                          decimal {
                            scale: 9
                            precision: 38
                          }
                        }
                      }
                    }
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_nullable_arrayType() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("arraynullable")
        .nullable()
        .typeArray()
        .typeBool()
        .field("arraynullableelement")
        .typeArray()
        .nullable()
        .typeInt64()
        .endStruct();

    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "arraynullable"
              names: "arraynullableelement"
              struct {
                types {
                  list {
                    type {
                      bool {
                      }
                    }
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  list {
                    type {
                      i64 {
                        nullability: NULLABILITY_NULLABLE
                      }
                    }
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_required_arrayType() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("arrayrequired")
        .required()
        .typeArray()
        .typeBool()
        .field("arrayrequiredelement")
        .typeArray()
        .required()
        .typeInt64()
        .endStruct();

    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "arrayrequired"
              names: "arrayrequiredelement"
              struct {
                types {
                  list {
                    type {
                      bool {
                      }
                    }
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  list {
                    type {
                      i64 {
                        nullability: NULLABILITY_REQUIRED
                      }
                    }
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_structType() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("struct")
        .typeStruct()
        .field("double")
        .typeDouble()
        .field("string")
        .typeString()
        .endStruct()
        .field("nestedstruct")
        .typeStruct()
        .field("int64")
        .typeInt64()
        .field("innerstruct")
        .typeStruct()
        .field("innerboolean")
        .typeBool()
        .endStruct()
        .endStruct()
        .field("anotherstruct")
        .typeStruct()
        .field("numeric")
        .typeNumeric()
        .endStruct()
        .endStruct();

    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "struct"
              names: "double"
              names: "string"
              names: "nestedstruct"
              names: "int64"
              names: "innerstruct"
              names: "innerboolean"
              names: "anotherstruct"
              names: "numeric"
              struct {
                types {
                  struct {
                    types {
                      fp64 {
                      }
                    }
                    types {
                      string {
                      }
                    }
                  }
                }
                types {
                  struct {
                    types {
                      i64 {
                      }
                    }
                    types {
                      struct {
                        types {
                          bool {
                          }
                        }
                      }
                    }
                  }
                }
                types {
                  struct {
                    types {
                      decimal {
                        scale: 9
                        precision: 38
                      }
                    }
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_nullable_structType() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("nullablestruct")
        .nullable()
        .typeStruct()
        .field("double")
        .typeDouble()
        .field("string")
        .typeString()
        .endStruct()
        .field("nestedstruct")
        .typeStruct()
        .field("nullableinteger")
        .nullable()
        .typeInt64()
        .endStruct()
        .endStruct();

    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "nullablestruct"
              names: "double"
              names: "string"
              names: "nestedstruct"
              names: "nullableinteger"
              struct {
                types {
                  struct {
                    types {
                      fp64 {
                      }
                    }
                    types {
                      string {
                      }
                    }
                    nullability: NULLABILITY_NULLABLE
                  }
                }
                types {
                  struct {
                    types {
                      i64 {
                        nullability: NULLABILITY_NULLABLE
                      }
                    }
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void buildNamedStruct_required_structType() throws ParseException {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("nullablestruct")
        .required()
        .typeStruct()
        .field("double")
        .typeDouble()
        .field("string")
        .typeString()
        .endStruct()
        .field("nestedstruct")
        .typeStruct()
        .field("nullableinteger")
        .required()
        .typeInt64()
        .endStruct()
        .endStruct();

    NamedStruct namedStruct = builder.createNamedStruct();

    NamedStruct expected =
        TextFormat.parse(
            """
              names: "nullablestruct"
              names: "double"
              names: "string"
              names: "nestedstruct"
              names: "nullableinteger"
              struct {
                types {
                  struct {
                    types {
                      fp64 {
                      }
                    }
                    types {
                      string {
                      }
                    }
                    nullability: NULLABILITY_REQUIRED
                  }
                }
                types {
                  struct {
                    types {
                      i64 {
                        nullability: NULLABILITY_REQUIRED
                      }
                    }
                  }
                }
              }
            """,
            NamedStruct.class);

    assertThat(namedStruct).isEqualTo(expected);
  }

  @Test
  public void failure_emptyStructField() {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    assertThrows(IllegalStateException.class, builder::endStruct);
  }

  @Test
  public void failure_missingEndStruct() {
    SubstraitSchemaBuilder builder = new SubstraitSchemaBuilder();
    builder
        .field("int64")
        .typeInt64()
        .field("struct")
        .typeStruct()
        .field("bool")
        .typeBool()
        .endStruct();

    assertThrows(IllegalStateException.class, builder::createNamedStruct);
  }
}
