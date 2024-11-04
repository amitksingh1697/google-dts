package com.google.cloud.connector.api;

import com.google.cloud.connector.api.data.RecordBuilder;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import java.io.Closeable;
import java.io.IOException;

/** An interface for reading records from the data source. */
public interface RecordReader extends Closeable {
  /**
   * Fetches the schema of the record.
   *
   * @param schemaBuilder the builder for creating the record schema.
   */
  void getRecordSchema(SchemaBuilder schemaBuilder);

  /**
   * Invoked by the platform to read a set of records. Individual record should be built by the
   * provided {@link RecordBuilder}. A record is being built and captured by the platform when the
   * {@link RecordBuilder#endStruct()} method is called.
   *
   * @param recordBuilder for building individual record
   * @return {@code true} to indicate there are more records, or {@code false} otherwise. This
   *     method will be called again by the platform if {@code true} is returned.
   * @throws IOException if there is an exception in reading
   */
  boolean nextRecord(RecordBuilder recordBuilder) throws IOException;
}
