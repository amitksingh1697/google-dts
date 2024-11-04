package com.google.cloud.connector.maven.sources;

import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_COMPUTE;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_FILTER;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_JOIN;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_LIMIT;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_OFFSET;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_SORTING;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_SOURCE_NESTING;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_TYPE_CAST;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.NativeQuerySchemaResolver;
import com.google.cloud.connector.api.ParallelQueryExecutor;
import com.google.cloud.connector.api.ParallelQueryPreparationContext;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import java.io.IOException;

public final class AllCapabilitiesDataSource
    implements Connector,
        NativeQuerySchemaResolver,
        ParallelQueryExecutor,
        SynchronousQueryExecutor {
  @DataSource(
      capabilities = {
        SUPPORTS_SOURCE_NESTING,
        SUPPORTS_LIMIT,
        SUPPORTS_OFFSET,
        SUPPORTS_SORTING,
        SUPPORTS_TYPE_CAST,
        SUPPORTS_JOIN,
        SUPPORTS_FILTER,
        SUPPORTS_COMPUTE
      })
  public AllCapabilitiesDataSource() {}

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resolveSchema(AssetName assetName, String nativeQuery, SchemaBuilder schemaBuilder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void prepareQuery(
      AssetName assetName, DataQuery query, ParallelQueryPreparationContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordReader readPartition(AssetName assetName, byte[] partitionData) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) throws IOException {
    throw new UnsupportedOperationException();
  }
}