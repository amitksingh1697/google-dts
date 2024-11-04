package com.google.cloud.connector.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.google.cloud.connector.api.BrowseAsset;
import com.google.cloud.connector.api.BrowseRequest;
import com.google.cloud.connector.api.BrowseResponse;
import com.google.cloud.connector.api.DataExploreServiceGrpc;
import com.google.cloud.connector.maven.ConnectorGeneratorMojo;
import com.google.cloud.connector.server.connector.ErrorConnector;
import com.google.cloud.connector.server.util.JarBuilder;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Guice;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.TextFormat.ParseException;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Unit-tests for the {@link ConnectorServer}.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ConnectorServerErrorsTest {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @TempDir
  private static Path connectorTopRootDir;

  private static ConnectorServer server;
  private static ConnectorServiceClient connectorServiceClient;
  private static ReaderServiceClient readerServiceClient;
  private static DataExploreServiceClient dataExploreServiceClient;

  @BeforeAll
  void setUp() throws Exception {
    Path classBasePath = getClassBasePath(ErrorConnector.class);
    Path connectorJar =
        buildConnectorJar(ErrorConnector.DATASOURCE_ID, ErrorConnector.class, classBasePath);
    logger.atInfo().log(
        "Connector '%s' jar has been built at '%s'", ErrorConnector.DATASOURCE_ID, connectorJar);

    CommandLine cli = new CommandLine(new ConnectorModule());
    cli.parseArgs(
        "--port", "0", "--insecure", "--connector-root-dir", connectorTopRootDir.toString());
    ConnectorModule connectorModule = cli.getCommand();
    server = Guice.createInjector(connectorModule).getInstance(ConnectorServer.class);
    server.startAsync().awaitRunning();

    int port = server.getPort();
    logger.atInfo().log("Connector server has started at port %d", port);

    connectorServiceClient = new ConnectorServiceClient(port);
    readerServiceClient = new ReaderServiceClient(port);
    dataExploreServiceClient = new DataExploreServiceClient(port);
  }

  @AfterAll
  void teardown() throws InterruptedException {
    connectorServiceClient.shutdown();
    server.stopAsync().awaitTerminated();
    logger.atInfo().log("Connector server has been shut down");
  }

  @Test
  public void getSchema_returnsWellFormattedException() throws InvalidProtocolBufferException {
    String datasourceId = ErrorConnector.DATASOURCE_ID;
    String databaseName = ErrorConnector.DATABASE_NAME;
    String tableId = ErrorConnector.TABLE_NAME_PERSON;
    ResolveSchemaRequest request =
        ResolveSchemaRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setNamedTable(NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        ErrorConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(100).build())
                    .build())
            .build();

    // Capture the Status Runtime Exception and check status code.
    StatusRuntimeException ex =
        assertThrows(
            StatusRuntimeException.class, () -> connectorServiceClient.retriveSchema(request));
    assertEquals(Code.INVALID_ARGUMENT, ex.getStatus().getCode());

    // Check the status information
    com.google.rpc.Status status = io.grpc.protobuf.StatusProto.fromThrowable(ex);
    assertEquals(Code.INVALID_ARGUMENT.value(), status.getCode());
  }

  @Test
  public void executeQuery_returnsWellFormattedException() throws InvalidProtocolBufferException {
    String datasourceId = ErrorConnector.DATASOURCE_ID;
    String databaseName = ErrorConnector.DATABASE_NAME;
    String tableId = ErrorConnector.TABLE_NAME_PERSON;
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
                        ErrorConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(numRows).build())
                    .build())
            .build();

    // Capture the Status Runtime Exception and check status code.
    StatusRuntimeException ex =
        assertThrows(
            StatusRuntimeException.class,
            () -> connectorServiceClient.executeQuery(request).hasNext());
    assertEquals(Code.INTERNAL, ex.getStatus().getCode());

    // Check the status information
    com.google.rpc.Status status = io.grpc.protobuf.StatusProto.fromThrowable(ex);
    assertEquals(Code.INTERNAL.value(), status.getCode());
  }

  @Test
  public void startQueryAndReadStream_returnsWellFormattedException()
      throws ParseException, InvalidProtocolBufferException {
    String datasourceId = ErrorConnector.DATASOURCE_ID;
    String databaseName = ErrorConnector.DATABASE_NAME;
    String tableId = ErrorConnector.TABLE_NAME_PERSON;
    int numRows = 3;

    // StartQuery phase.
    StartQueryRequest startQueryReq =
        StartQueryRequest.newBuilder()
            .setDataset(getDatasetName(datasourceId))
            .setDataQuery(DataQuery.newBuilder()
                .setNamedTable(
                    NamedTable.newBuilder().addNames(databaseName).addNames(tableId).build())
                .build())
            .setParameters(
                Struct.newBuilder()
                    .putFields(
                        ErrorConnector.CONFIG_KEY_NUM_ROWS,
                        Value.newBuilder().setNumberValue(numRows).build())
                    .build())
            .build();

    // Capture the Status Runtime Exception and check status code.
    StatusRuntimeException ex =
        assertThrows(
            StatusRuntimeException.class, () -> connectorServiceClient.startQuery(startQueryReq));
    assertEquals(Code.UNAVAILABLE, ex.getStatus().getCode());

    // Check the status information
    com.google.rpc.Status status = io.grpc.protobuf.StatusProto.fromThrowable(ex);
    assertEquals(Code.UNAVAILABLE.value(), status.getCode());
  }

  @Test
  public void browse_returnsWellFormattedException() throws Exception {
    // Browse from root level to get databases
    String dataSource = String.format("datasources/%s", ErrorConnector.DATASOURCE_ID);
    BrowseRequest request =
        BrowseRequest.newBuilder()
            .setAsset(BrowseAsset.newBuilder().setDataset(dataSource))
            .build();

    // Capture the Status Runtime Exception and check status code.
    StatusRuntimeException ex =
        assertThrows(StatusRuntimeException.class, () -> dataExploreServiceClient.browse(request));
    assertEquals(Code.PERMISSION_DENIED, ex.getStatus().getCode());

    // Check the status information
    com.google.rpc.Status status = io.grpc.protobuf.StatusProto.fromThrowable(ex);
    assertEquals(Code.PERMISSION_DENIED.value(), status.getCode());
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

    public Schema retriveSchema(ResolveSchemaRequest request) {
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

  private static Path buildConnectorJar(String connectorName, Class<?> clazz, Path classBasePath)
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
