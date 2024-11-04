package com.google.cloud.connector.server;

import static com.google.cloud.connector.data.ResultStreamId.COLLECTION_ID_RESULTSET;
import static com.google.cloud.connector.data.ResultStreamId.COLLECTION_ID_STREAM;
import static com.google.common.truth.Truth.assertThat;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static io.grpc.Status.Code.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.cloud.bigquery.federation.v1alpha1.ConnectorServiceGrpc;
import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.bigquery.federation.v1alpha1.DataSource;
import com.google.cloud.bigquery.federation.v1alpha1.ExecuteQueryRequest;
import com.google.cloud.bigquery.federation.v1alpha1.GetDataSourceRequest;
import com.google.cloud.bigquery.federation.v1alpha1.NamedTable;
import com.google.cloud.bigquery.federation.v1alpha1.ReadStreamRequest;
import com.google.cloud.bigquery.federation.v1alpha1.ReaderServiceGrpc;
import com.google.cloud.bigquery.federation.v1alpha1.ResolveSchemaRequest;
import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryRequest;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse.ResultSet;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse.ResultSet.Stream;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.BrowseAsset;
import com.google.cloud.connector.api.BrowseRequest;
import com.google.cloud.connector.api.BrowseResponse;
import com.google.cloud.connector.api.DataExploreServiceGrpc;
import com.google.cloud.connector.api.DatasetName;
import com.google.cloud.connector.data.AssetInfo;
import com.google.cloud.connector.data.Partition;
import com.google.cloud.connector.data.ResultStreamId;
import com.google.cloud.connector.maven.ConnectorGeneratorMojo;
import com.google.cloud.connector.server.connector.InMemDatabaseConnector;
import com.google.cloud.connector.server.util.JarBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.protobuf.Struct;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import com.google.protobuf.Value;
import com.google.testing.junit.testparameterinjector.junit5.TestParameter;
import com.google.testing.junit.testparameterinjector.junit5.TestParameterInjectorTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Unit-tests for the {@link ConnectorServer}.
 */
public class ConnectorServerTest {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private ConnectorServer server;
  private ConnectorServiceClient connectorServiceClient;
  private ReaderServiceClient readerServiceClient;
  private DataExploreServiceClient dataExploreServiceClient;
  private static final Gson GSON = new Gson();

  @TestParameter
  private boolean enableLoaderCache;

  @BeforeEach
  void setUp(@TempDir Path connectorTopRootDir, TestInfo testInfo) throws Exception {
    Path classBasePath = getClassBasePath(InMemDatabaseConnector.class);
    Path connectorJar =
        buildConnectorJar(
            InMemDatabaseConnector.DATASOURCE_ID,
            InMemDatabaseConnector.class,
            classBasePath,
            connectorTopRootDir);
    logger.atInfo().log(
        "Connector '%s' jar has been built at '%s'",
        InMemDatabaseConnector.DATASOURCE_ID, connectorJar);

    List<String> args = new ArrayList<>();
    args.add("--port");
    args.add("0");
    args.add("--insecure");
    args.add("--connector-root-dir");
    args.add(connectorTopRootDir.toString());
    args.add("--enable-loader-cache=" + enableLoaderCache);

    // Add extra arguments from the test
    args.addAll(testInfo.getTags());

    CommandLine cli = new CommandLine(new ConnectorModule());
    cli.parseArgs(args.toArray(new String[0]));
    ConnectorModule connectorModule = cli.getCommand();
    server = Guice.createInjector(connectorModule).getInstance(ConnectorServer.class);

    server.startAsync().awaitRunning();
    int port = server.getPort();
    logger.atInfo().log("Connector server has started at port %d", port);

    connectorServiceClient = new ConnectorServiceClient(port);
    readerServiceClient = new ReaderServiceClient(port);
    dataExploreServiceClient = new DataExploreServiceClient(port);
  }

  @AfterEach
  void teardown() throws InterruptedException {
    connectorServiceClient.shutdown();
    server.stopAsync().awaitTerminated();
    logger.atInfo().log("Connector server has been shut down");
  }

  @TestParameterInjectorTest
  public void getDatasource_InMemoryDatabaseConnector() throws ParseException {
    GetDataSourceRequest request =
        GetDataSourceRequest.newBuilder()
            .setName("datasources/" + InMemDatabaseConnector.DATASOURCE_ID)
            .build();
    DataSource dataSource = connectorServiceClient.getDataSource(request);

    DataSource expectedDataSource =
        TextFormat.parse(
            """
            name: "datasources/inmemdb"
            id: "inmemdb"
            capabilities: SUPPORTS_NATIVE_QUERIES
            capabilities: SUPPORTS_PARALLEL_QUERIES
            capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
            parameters {
              name: "datasources/inmemdb/parameters/numRows"
              id: "numRows"
              type {
                type_kind: INT32
              }
            }
            """,
            DataSource.class);
    assertThat(dataSource).isEqualTo(expectedDataSource);
  }

  @TestParameterInjectorTest
  public void fail_getDataSource_wrongNameFormat() {
    GetDataSourceRequest request =
        GetDataSourceRequest.newBuilder().setName(InMemDatabaseConnector.DATASOURCE_ID).build();
    StatusRuntimeException exception =
        assertThrows(
            StatusRuntimeException.class, () -> connectorServiceClient.getDataSource(request));
    assertThat(exception.getStatus().getCode()).isEqualTo(INVALID_ARGUMENT);
  }

  @TestParameterInjectorTest
  public void fail_getDataSource_missingDataSource() {
    GetDataSourceRequest request =
        GetDataSourceRequest.newBuilder().setName("datasources/missing").build();
    StatusRuntimeException exception =
        assertThrows(
            StatusRuntimeException.class, () -> connectorServiceClient.getDataSource(request));
    assertThat(exception.getStatus().getCode()).isEqualTo(NOT_FOUND);
  }

  @TestParameterInjectorTest
  public void getSchema_InMemoryDatabaseConnector_returnValidTableSchema() throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNamedTable(NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();
    Schema schema = connectorServiceClient.retrieveSchema(request);

    Schema expectedSchema =
        TextFormat.parse(
            """
            name: "person"
            fields {
              field_name: "firstName"
              type {
                type_kind:
                STRING
              }
            }
            fields {
              field_name: "lastName"
              type {
                type_kind: STRING
              }
            }
            fields {
              field_name: "age"
            type {
                type_kind: INT64
              }
            }
            data_source {
              name: "datasources/inmemdb"
              id: "inmemdb"
              capabilities: SUPPORTS_NATIVE_QUERIES
              capabilities: SUPPORTS_PARALLEL_QUERIES
              capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
              parameters {
                name: "datasources/inmemdb/parameters/numRows"
                id: "numRows"
                type {
                  type_kind: INT32
                }
              }
            }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expectedSchema);
  }

  @TestParameterInjectorTest
  public void getSchema_InMemoryDatabaseConnector_returnValidNamedTableSchema()
      throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNamedTable(NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();
    Schema schema = connectorServiceClient.retrieveSchema(request);

    Schema expectedSchema =
        TextFormat.parse(
            """
            name: "person"
            fields {
              field_name: "firstName"
              type {
                type_kind:
                STRING
              }
            }
            fields {
              field_name: "lastName"
              type {
                type_kind: STRING
              }
            }
            fields {
              field_name: "age"
            type {
                type_kind: INT64
              }
            }
            data_source {
              name: "datasources/inmemdb"
              id: "inmemdb"
              capabilities: SUPPORTS_NATIVE_QUERIES
              capabilities: SUPPORTS_PARALLEL_QUERIES
              capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
              parameters {
                name: "datasources/inmemdb/parameters/numRows"
                id: "numRows"
                type {
                  type_kind: INT32
                }
              }
            }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expectedSchema);
  }

  @TestParameterInjectorTest
  @Tag("--substrait")
  public void getSubstraitSchema_InMemoryDatabaseConnector_returnValidTableSchema()
      throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNamedTable(NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();
    Schema schema = connectorServiceClient.retrieveSchema(request);

    Schema expectedSchema =
        TextFormat.parse(
            """
            substrait_schema {
              schema {
                names: "firstName"
                names: "lastName"
                names: "age"
                struct {
                  types {
                    string {
                    }
                  }
                  types {
                    string {
                    }
                  }
                  types {
                    i64 {
                    }
                  }
                }
              }
            }
            data_source {
              name: "datasources/inmemdb"
              id: "inmemdb"
              capabilities: SUPPORTS_NATIVE_QUERIES
              capabilities: SUPPORTS_PARALLEL_QUERIES
              capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
              parameters {
                name: "datasources/inmemdb/parameters/numRows"
                id: "numRows"
                type {
                  type_kind: INT32
                }
              }
            }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expectedSchema);
  }

  @TestParameterInjectorTest
  public void getSchema_InMemoryDatabaseConnector_returnValidAssetSchema() throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNamedTable(NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();
    Schema schema = connectorServiceClient.retrieveSchema(request);

    Schema expectedSchema =
        TextFormat.parse(
            """
            name: "person"
            fields {
              field_name: "firstName"
              type {
                type_kind: STRING
              }
            }
            fields {
              field_name: "lastName"
              type {
                type_kind: STRING
              }
            }
            fields {
              field_name: "age"
            type {
                type_kind: INT64
              }
            }
            data_source {
              name: "datasources/inmemdb"
              id: "inmemdb"
              capabilities: SUPPORTS_NATIVE_QUERIES
              capabilities: SUPPORTS_PARALLEL_QUERIES
              capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
              parameters {
                name: "datasources/inmemdb/parameters/numRows"
                id: "numRows"
                type {
                  type_kind: INT32
                }
              }
            }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expectedSchema);
  }

  @TestParameterInjectorTest
  @Tag("--substrait")
  public void getSubstraitSchema_InMemoryDatabaseConnector_returnValidAssetSchema()
      throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNamedTable(NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();
    Schema schema = connectorServiceClient.retrieveSchema(request);

    Schema expectedSchema =
        TextFormat.parse(
            """
            substrait_schema {
              schema {
                names: "firstName"
                names: "lastName"
                names: "age"
                struct {
                  types {
                    string {
                    }
                  }
                  types {
                    string {
                    }
                  }
                  types {
                    i64 {
                    }
                  }
                }
              }
            }
            data_source {
              name: "datasources/inmemdb"
              id: "inmemdb"
              capabilities: SUPPORTS_NATIVE_QUERIES
              capabilities: SUPPORTS_PARALLEL_QUERIES
              capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
              parameters {
                name: "datasources/inmemdb/parameters/numRows"
                id: "numRows"
                type {
                  type_kind: INT32
                }
              }
            }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expectedSchema);
  }

  @TestParameterInjectorTest
  public void getSchema_InMemoryDatabaseConnector_returnValidNativeQuerySchema()
      throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNativeQuery("SELECT * FROM person")
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();
    Schema schema = connectorServiceClient.retrieveSchema(request);

    Schema expectedSchema =
        TextFormat.parse(
            """
            name: "person"
            fields {
              field_name: "firstName"
              type {
                type_kind: STRING
              }
            }
            fields {
              field_name: "lastName"
              type {
                type_kind: STRING
              }
            }
            fields {
              field_name: "age"
            type {
                type_kind: INT64
              }
            }
            data_source {
              name: "datasources/inmemdb"
              id: "inmemdb"
              capabilities: SUPPORTS_NATIVE_QUERIES
              capabilities: SUPPORTS_PARALLEL_QUERIES
              capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
              parameters {
                name: "datasources/inmemdb/parameters/numRows"
                id: "numRows"
                type {
                  type_kind: INT32
                }
              }
            }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expectedSchema);
  }

  @TestParameterInjectorTest
  @Tag("--substrait")
  public void getSubstraitSchema_InMemoryDatabaseConnector_returnValidNativeQuerySchema()
      throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNativeQuery("SELECT * FROM person")
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();
    Schema schema = connectorServiceClient.retrieveSchema(request);

    Schema expectedSchema =
        TextFormat.parse(
            """
            substrait_schema {
              schema {
                names: "firstName"
                names: "lastName"
                names: "age"
                struct {
                  types {
                    string {
                    }
                  }
                  types {
                    string {
                    }
                  }
                  types {
                    i64 {
                    }
                  }
                }
              }
            }
            data_source {
              name: "datasources/inmemdb"
              id: "inmemdb"
              capabilities: SUPPORTS_NATIVE_QUERIES
              capabilities: SUPPORTS_PARALLEL_QUERIES
              capabilities: SUPPORTS_SYNCHRONOUS_QUERIES
              parameters {
                name: "datasources/inmemdb/parameters/numRows"
                id: "numRows"
                type {
                  type_kind: INT32
                }
              }
            }
            """,
            Schema.class);
    assertThat(schema).isEqualTo(expectedSchema);
  }

  @TestParameterInjectorTest
  public void executeQuery_InMemoryDatabaseConnector_ForNamedTable_returnValidData()
      throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    int numRows = 3;
    ExecuteQueryRequest request =
        ExecuteQueryRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setDataQuery(
                DataQuery.newBuilder()
                    .setNamedTable(
                        NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
                    .build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(numRows).build())
                    .build())
            .build();

    int dataChunkCount = 0;
    Iterator<Data> iterator = connectorServiceClient.executeQuery(request);
    while (iterator.hasNext()) {
      Data data = iterator.next();
      if (dataChunkCount == 0) {
        assertThat(data.hasSchema()).isTrue();
        assertThat(data.getDataQuery()).isEqualTo(DataQuery.getDefaultInstance());
      } else {
        assertThat(data.hasSchema()).isFalse();
        assertThat(data.hasDataQuery()).isFalse();
      }
      assertThat(data.getDataBlock().getRowset().getRowsCount()).isEqualTo(numRows);

      dataChunkCount++;
    }
    assertThat(dataChunkCount).isEqualTo(1);
  }

  @TestParameterInjectorTest
  public void executeQuery_InMemoryDatabaseConnector_ForTable_returnValidData()
      throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    int numRows = 3;
    ExecuteQueryRequest request =
        ExecuteQueryRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setDataQuery(DataQuery.newBuilder()
                .setNamedTable(
                    NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
                .build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(numRows).build())
                    .build())
            .build();

    int dataChunkCount = 0;
    Iterator<Data> iterator = connectorServiceClient.executeQuery(request);
    while (iterator.hasNext()) {
      Data data = iterator.next();
      if (dataChunkCount == 0) {
        assertThat(data.hasSchema()).isTrue();
        assertThat(data.getDataQuery()).isEqualTo(DataQuery.getDefaultInstance());
      } else {
        assertThat(data.hasSchema()).isFalse();
        assertThat(data.hasDataQuery()).isFalse();
      }
      assertThat(data.getDataBlock().getRowset().getRowsCount()).isEqualTo(numRows);

      dataChunkCount++;
    }
    assertThat(dataChunkCount).isEqualTo(1);
  }

  @TestParameterInjectorTest
  public void startQueryAndReadStream_InMemoryDatabaseConnector_succeed() throws ParseException {
    String datasourceId = InMemDatabaseConnector.DATASOURCE_ID;
    String databaseName = InMemDatabaseConnector.DATABASE_NAME;
    String tableId = InMemDatabaseConnector.TABLE_NAME_PERSON;
    int numRows = 3;

    // StartQuery phase.
    StartQueryRequest startQueryReq =
        StartQueryRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setDataQuery(
                DataQuery.newBuilder()
                    .setNamedTable(
                        NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
                    .build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(numRows).build())
                    .build())
            .build();

    StartQueryResponse startQueryResp = connectorServiceClient.startQuery(startQueryReq);

    assertThat(startQueryResp.getMaxConcurrency())
        .isEqualTo(InMemDatabaseConnector.QUERY_MAX_CONCURRENCY);
    assertThat(startQueryResp.getResultSetsList().size()).isEqualTo(1);

    ResultSet resultSet = startQueryResp.getResultSets(0);
    DatasetName resultSetName = DatasetName.fromName(resultSet.getName());
    assertThat(resultSetName.hasComponent(COLLECTION_ID_RESULTSET)).isTrue();
    assertThat(resultSet.getStreamsCount()).isEqualTo(1);

    Stream stream = resultSet.getStreams(0);
    DatasetName streamName = DatasetName.fromName(stream.getName());
    assertThat(streamName.hasComponent(COLLECTION_ID_STREAM)).isTrue();

    String streamId = streamName.getComponent(COLLECTION_ID_STREAM).resourceId();
    assertThat(streamId).isNotNull();

    String partitionData = new String(ResultStreamId.decode(streamId), StandardCharsets.UTF_8);
    Partition partition = GSON.fromJson(partitionData, Partition.class);
    assertNotNull(partition.query());
    String query = new String(partition.query(), StandardCharsets.UTF_8);
    assertThat(query).isEqualTo(tableId);

    // ReadStream phase.
    ReadStreamRequest readStreamReq =
        ReadStreamRequest.newBuilder()
            .setResultStream(stream.getName())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(numRows).build())
                    .build())
            .build();

    int dataChunkCount = 0;
    Iterator<Data> iterator = readerServiceClient.readStream(readStreamReq);
    while (iterator.hasNext()) {
      Data data = iterator.next();
      if (dataChunkCount == 0) {
        assertThat(data.hasSchema()).isTrue();
        assertThat(data.getDataQuery()).isEqualTo(DataQuery.getDefaultInstance());
      } else {
        assertThat(data.hasSchema()).isFalse();
        assertThat(data.hasDataQuery()).isFalse();
      }
      assertThat(data.getDataBlock().getRowset().getRowsCount()).isEqualTo(numRows);

      dataChunkCount++;
    }
    assertThat(dataChunkCount).isEqualTo(1);
  }

  @TestParameterInjectorTest
  public void readStream_EmptyStream_IsSuccessful() {
    Partition emptyPartition = new Partition(
        null, new AssetInfo(new AssetName(ImmutableList.of("DB", "person")), null));
    String encodedPartition = Base64.getUrlEncoder().encodeToString(
        GSON.toJson(emptyPartition).getBytes(StandardCharsets.UTF_8));
    // ReadStream phase.
    ReadStreamRequest readStreamReq =
        ReadStreamRequest.newBuilder()
            .setResultStream(
                String.format("datasources/inmemdb/resultsets/single-resultset/streams/%s",
                    encodedPartition))
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        InMemDatabaseConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(0).build())
                    .build())
            .build();

    Iterator<Data> iterator = readerServiceClient.readStream(readStreamReq);
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next().hasSchema()).isTrue();
    assertThat(iterator.hasNext()).isFalse();
  }


  @TestParameterInjectorTest
  public void browse_InMemoryDatabaseConnector_success() {
    // Browse from root level to get databases
    String dataset =
        String.format("datasources/%s/instances/instance", InMemDatabaseConnector.DATASOURCE_ID);
    BrowseAsset parentAsset = BrowseAsset.newBuilder().setDataset(dataset).build();
    BrowseRequest request = BrowseRequest.newBuilder().setAsset(parentAsset).build();
    BrowseResponse response = dataExploreServiceClient.browse(request);

    BrowseAsset expectedAsset = InMemDatabaseConnector.DATABASE_ASSET;
    assertThat(response.getParentAsset()).isEqualTo(parentAsset);
    assertThat(response.getAssetsList())
        .isEqualTo(
            Collections.singletonList(
                BrowseAsset.newBuilder(expectedAsset)
                    .setDataset(dataset)
                    .setNamedTable(expectedAsset.getNamedTable())
                    .build()));

    // Browse from databases to get tables
    parentAsset = response.getAssets(0);
    request = BrowseRequest.newBuilder().setAsset(parentAsset).build();
    response = dataExploreServiceClient.browse(request);
    assertThat(response.getParentAsset()).isEqualTo(request.getAsset());
    List<BrowseAsset> expected =
        java.util.stream.Stream.of(
                InMemDatabaseConnector.TABLE_ASSET_PERSON,
                InMemDatabaseConnector.TABLE_ASSET_CUSTOMER)
            .map(
                browseAsset ->
                    BrowseAsset.newBuilder(browseAsset)
                        .setDataset(dataset)
                        .setNamedTable(browseAsset.getNamedTable())
                        .build())
            .toList();
    assertThat(response.getAssetsList()).isEqualTo(expected);
  }

  @TestParameterInjectorTest
  public void browseUsingNamedTable_InMemoryDatabaseConnector_success() {
    // Browse from root level to get databases
    String dataSource = String.format("datasources/%s", InMemDatabaseConnector.DATASOURCE_ID);
    BrowseAsset parentAsset =
        BrowseAsset.newBuilder()
            .setDataset(dataSource)
            .setNamedTable(NamedTable.getDefaultInstance())
            .build();
    BrowseRequest request = BrowseRequest.newBuilder().setAsset(parentAsset).build();
    BrowseResponse response = dataExploreServiceClient.browse(request);

    BrowseAsset expectedAsset = InMemDatabaseConnector.DATABASE_ASSET;
    assertThat(response.getParentAsset()).isEqualTo(parentAsset);
    assertThat(response.getAssetsList())
        .isEqualTo(
            Collections.singletonList(
                BrowseAsset.newBuilder(expectedAsset)
                    .setDataset(dataSource)
                    .setNamedTable(expectedAsset.getNamedTable())
                    .build()));

    // Browse from databases to get tables
    parentAsset = response.getAssets(0);
    request = BrowseRequest.newBuilder().setAsset(parentAsset).build();
    response = dataExploreServiceClient.browse(request);
    assertThat(response.getParentAsset()).isEqualTo(request.getAsset());
    List<BrowseAsset> expected =
        java.util.stream.Stream.of(
                InMemDatabaseConnector.TABLE_ASSET_PERSON,
                InMemDatabaseConnector.TABLE_ASSET_CUSTOMER)
            .map(
                browseAsset ->
                    BrowseAsset.newBuilder(browseAsset)
                        .setDataset(dataSource)
                        .setNamedTable(browseAsset.getNamedTable())
                        .build())
            .toList();
    assertThat(response.getAssetsList()).isEqualTo(expected);
  }

  /**
   * A client that sends requests to the connector server.
   */
  private static class ConnectorServiceClient {

    private final ManagedChannel channel;
    private final ConnectorServiceGrpc.ConnectorServiceBlockingStub blockingStub;

    public ConnectorServiceClient(int port) {
      this.channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();

      this.blockingStub = ConnectorServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public DataSource getDataSource(GetDataSourceRequest request) {
      return blockingStub.getDataSource(request);
    }

    public Schema retrieveSchema(ResolveSchemaRequest request) {
      return blockingStub.resolveSchema(request);
    }

    public Iterator<Data> executeQuery(ExecuteQueryRequest request) {
      return blockingStub.executeQuery(request);
    }

    public StartQueryResponse startQuery(StartQueryRequest request) {
      return blockingStub.startQuery(request);
    }
  }

  /**
   * A client that sends requests to the connector server.
   */
  private static class ReaderServiceClient {

    private final ReaderServiceGrpc.ReaderServiceBlockingStub blockingStub;

    public ReaderServiceClient(int port) {
      this.blockingStub =
          ReaderServiceGrpc.newBlockingStub(
              ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build());
    }

    Iterator<Data> readStream(ReadStreamRequest request) {
      return blockingStub.readStream(request);
    }
  }

  private static class DataExploreServiceClient {

    private final DataExploreServiceGrpc.DataExploreServiceBlockingStub blockingStub;

    public DataExploreServiceClient(int port) {
      this.blockingStub =
          DataExploreServiceGrpc.newBlockingStub(
              ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build());
    }

    BrowseResponse browse(BrowseRequest request) {
      return blockingStub.browse(request);
    }
  }

  private Path getClassBasePath(Class<?> cls) throws URISyntaxException {
    String classFile = cls.getName().replace('.', File.separatorChar) + ".class";
    URL resource = getClass().getClassLoader().getResource(classFile);
    assertNotNull(resource);
    String uriStr = resource.toURI().toString();
    return Paths.get(URI.create(uriStr.substring(0, uriStr.length() - classFile.length())));
  }

  private String getDatasetName(String datasourceId) {
    return String.format("datasources/%s", datasourceId);
  }

  private Path buildConnectorJar(
      String connectorName, Class<?> clazz, Path classBasePath, Path connectorTopRootDir)
      throws Exception {
    List<Path> classFiles = new ArrayList<>();
    Path classFileParentDir =
        classBasePath
            .resolve(Path.of(clazz.getName().replace('.', File.separatorChar) + ".class"))
            .getParent();
    for (File file : classFileParentDir.toFile().listFiles()) {
      if (file.isFile() && file.getName().startsWith(clazz.getSimpleName())) {
        classFiles.add(file.toPath());
      }
    }

    List<Path> resourceFiles = new ArrayList<>();
    ConnectorGeneratorMojo.create(
            classBasePath,
            classBasePath,
            clazz.getName(),
            LocalPathConnectorLoader.CONNECTOR_PROTO_FILE)
        .execute();
    resourceFiles.add(classBasePath.resolve(LocalPathConnectorLoader.CONNECTOR_PROTO_FILE));

    final Path connectorDir = Files.createDirectory(connectorTopRootDir.resolve(connectorName));
    final Path connectorJar = connectorDir.resolve(connectorName + ".jar");
    JarBuilder jarBuilder = new JarBuilder(connectorJar);
    jarBuilder
        .addClassPaths(classFiles, classBasePath)
        .addResourcePaths(resourceFiles, classBasePath)
        .build();
    return connectorJar;
  }
}
