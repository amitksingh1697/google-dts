package com.google.cloud.connector.schema;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ConnectorSchemaBuilder}. */
public class ConnectorSchemaBuilderTest {

  @Test
  public void buildSchemaWithSimpleType_returnValidSchema() throws ParseException {
    ConnectorSchemaBuilder builder = new ConnectorSchemaBuilder();
    builder
        .name("my_table")
        .field("string_type")
        .typeString()
        .field("int64_type")
        .typeInt64()
        .field("int32_type")
        .typeInt32()
        .field("uint64_type")
        .typeUint64()
        .field("uint32_type")
        .typeUint32()
        .field("float_type")
        .typeFloat()
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
        .typeBytes()
        .field("time_type")
        .typeTime()
        .field("geography_type")
        .typeGeography()
        .field("numeric_type")
        .typeNumeric()
        .field("bignumeric")
        .typeBigNumeric()
        .field("interval_type")
        .typeInterval()
        .field("json_type")
        .typeJson()
        .endStruct();
    Schema schema = builder.createSchema();

    Schema expected =
        TextFormat.parse(
            """
              name: "my_table"
              fields {
                field_name: "string_type"
                type {
                  type_kind: STRING
                }
              }
              fields {
                field_name: "int64_type"
                type {
                  type_kind: INT64
                }
              }
              fields {
                field_name: "int32_type"
                type {
                  type_kind: INT32
                }
              }
              fields {
                field_name: "uint64_type"
                type {
                  type_kind: UINT64
                }
              }
              fields {
                field_name: "uint32_type"
                type {
                  type_kind: UINT32
                }
              }
              fields {
                field_name: "float_type"
                type {
                  type_kind: FLOAT
                }
              }
              fields {
                field_name: "double_type"
                type {
                  type_kind: DOUBLE
                }
              }
              fields {
                field_name: "bool_type"
                type {
                  type_kind: BOOL
                }
              }
              fields {
                field_name: "date_type"
                type {
                  type_kind: DATE
                }
              }
              fields {
                field_name: "datetime_type"
                type {
                  type_kind: DATETIME
                }
              }
              fields {
                field_name: "bytes_type"
                type {
                  type_kind: BYTES
                }
              }
              fields {
                field_name: "timestamp_type"
                type {
                  type_kind: BYTES
                }
              }
              fields {
                field_name: "time_type"
                type {
                  type_kind: TIME
                }
              }
              fields {
                field_name: "geography_type"
                type {
                  type_kind: GEOGRAPHY
                }
              }
              fields {
                field_name: "numeric_type"
                type {
                  type_kind: NUMERIC
                }
              }
              fields {
                field_name: "bignumeric"
                type {
                  type_kind: BIGNUMERIC
                }
              }
              fields {
                field_name: "interval_type"
                type {
                  type_kind: INTERVAL
                }
              }
              fields {
                field_name: "json_type"
                type {
                  type_kind: JSON
                }
              }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expected);
  }

  @Test
  public void buildSchemaWithArrayType_returnValidSchema() throws ParseException {
    ConnectorSchemaBuilder builder = new ConnectorSchemaBuilder();
    builder.name("my_table").field("department_list").typeArray().typeString().endStruct();
    Schema schema = builder.createSchema();

    Schema expected =
        TextFormat.parse(
            """
              name: "my_table"
              fields {
                field_name: "department_list"
                type {
                  type_kind: ARRAY
                  array_type {
                    element_type {
                      type_kind: STRING
                    }
                  }
                }
              }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expected);
  }

  @Test
  public void buildSchemaWithStructType_returnValidSchema() throws ParseException {
    ConnectorSchemaBuilder builder = new ConnectorSchemaBuilder();
    builder
        .name("my_table")
        .field("employee")
        .typeStruct()
        .field("first_name")
        .typeString()
        .field("last_name")
        .typeString()
        .field("age")
        .typeInt64()
        .endStruct()
        .endStruct();
    Schema schema = builder.createSchema();

    Schema expected =
        TextFormat.parse(
            """
              name: "my_table"
              fields {
                field_name: "employee"
                type {
                  type_kind: STRUCT
                  struct_type {
                    fields {
                      field_name: "first_name"
                      field_type {
                        type_kind: STRING
                      }
                    }
                    fields {
                      field_name: "last_name"
                      field_type {
                        type_kind: STRING
                      }
                    }
                    fields {
                      field_name: "age"
                      field_type {
                        type_kind: INT64
                      }
                    }
                  }
                }
              }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expected);
  }

  @Test
  public void buildSchema_emptyField_throwsException() {
    ConnectorSchemaBuilder builder = new ConnectorSchemaBuilder();
    assertThrows(IllegalStateException.class, () -> builder.name("my_table").endStruct());
  }

  @Test
  public void buildSchemaWithStructType_emptySubField_throwsException() {
    ConnectorSchemaBuilder builder = new ConnectorSchemaBuilder();
    assertThrows(
        IllegalStateException.class,
        () -> builder.name("my_table").field("struct").typeStruct().endStruct());
  }

  @Test
  public void buildSchema_missingEndStructCall_throwsException() {
    ConnectorSchemaBuilder builder = new ConnectorSchemaBuilder();
    builder.name("my_table").field("int").typeInt64();

    assertThrows(IllegalStateException.class, builder::createSchema);
  }
}
