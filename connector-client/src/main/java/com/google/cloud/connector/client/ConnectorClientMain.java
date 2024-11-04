package com.google.cloud.connector.client;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.bigquery.federation.v1alpha1.DataSource;
import com.google.cloud.bigquery.federation.v1alpha1.NamedTable;
import com.google.cloud.bigquery.federation.v1alpha1.Parameter;
import com.google.cloud.bigquery.federation.v1alpha1.Schema;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse.ResultSet;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse.ResultSet.Stream;
import com.google.cloud.connector.api.BrowseAsset;
import com.google.cloud.connector.api.BrowseResponse;
import com.google.cloud.connector.client.LoadTester.Statistics;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Entry point for the Connector Client runtime.
 */
@CommandLine.Command()
public class ConnectorClientMain implements Callable {

  @Option(names = {"-s", "--server"},
      required = false,
      defaultValue = "127.0.0.1",
      description = "Server Hostname/IP running the connector service.")
  private String server;

  @Option(names = {"-p", "--port"},
      required = false,
      defaultValue = "54321",
      description = "Server port number where connector service is listening.")
  private String port;

  @Option(names = {"-d", "--datasource"},
      required = true,
      description = "Datasource Name")
  private String datasource;

  @Option(names = {"-ds", "--dataset"},
      required = false,
      description = "Dataset Name in format datasources/{datasource}")
  private String dataset;

  @Option(names = {"-nt", "--namedTable"},
      required = false,
      description = "NamedTable as {element}/{element}/... in DataQuery or source if required")
  private String namedTable;

  @Option(names = {"-nq", "--nativeQuery"},
      required = false,
      description = "Native Query in DataQuery or source if required")
  private String nativeQuery;

  @Option(names = {"-f", "--function"},
      required = true,
      description = "Function to test")
  private String function;

  @Option(names = {"-ps", "--partitionStream"},
      required = false,
      description = "Partition Stream name to read from")
  private String stream;

  @Option(names = {"-c", "--conf"},
      required = true,
      description = "Datasource configuration details, in json format")
  private String configuration;

  @Option(names = {"-m", "--mode"},
      required = false,
      defaultValue = "default",
      description = "Possible values : 'default', 'null'. "
          + "Use 'default' to generate default output data file for the run. "
          + "Use 'null' to generate null check output data file. ")
  private String mode;

  @Option(names = {"-g", "--generate"},
      required = false,
      description = "Generate data file. ")
  private boolean generate;

  @Option(names = {"-lt", "--loadTest"},
      required = false,
      description = "Run load test and generate size, latency data"
  )
  private boolean loadTest;

  /**
   * Do not use this variable directly, use getBuilder method instead.
   */
  private Struct.Builder builder;
  private static int runs = 1; // default, non-load test
  private static final String TEST_RESOURCES = "src/test/resources/";

  public static void main(String[] args) {
    new CommandLine(new ConnectorClientMain()).execute(args);
  }

  @Override
  public Object call() {
    if (generate && loadTest) {
      throw new IllegalArgumentException("Can only specify one of --generate or --loadTest flags.");
    }
    if (loadTest) {
      runs = LoadTester.NUM_RUNS;
    }
    execute();
    return null;
  }

  private void execute() {
    try {
      Duration elapsedTime = switch (function) {
        case "GetDataSource" -> runGetDataSource();
        case "BrowseAsset" -> runBrowseAsset();
        case "ResolveSchema" -> runResolveSchema();
        case "ExecuteQuery" -> runExecuteQuery();
        case "PrepareQuery" -> runPrepareQuery();
        case "ReadPartition" -> runReadPartition();
        case "AsyncQuery" -> runAsyncQuery();
        case "ReadRandomPartition" -> runReadRandomPartition();
        default -> throw new RuntimeException(
            String.format("Unknown option : '%s', Supported options are "
                + "GetDataSource, BrowseAsset, ResolveSchema,"
                + " ExecuteQuery, PrepareQuery, AsyncQuery,"
                + " ReadPartition and ReadRandomPartition", function));
      };
      if (!loadTest) {
        System.out.printf("Total runtime: %s%n", elapsedTime.toString());
      }
    } catch (Exception exception) {
      System.out.println(exception.getMessage());
    }
  }

  private Duration runGetDataSource() {
    Duration latency = Duration.ZERO;
    for (int i = 0; i < runs; i++) {
      Instant start = Instant.now();
      DataSource dataSource = getDatasource();
      latency = Duration.between(start, Instant.now());

      if (loadTest) {
        Statistics stats =
            Statistics.getBuilder(dataSource.getSerializedSize(), latency.toMillis()).build();
        LoadTester.writeStats(stats);
      } else {
        System.out.println(dataSource.toString());
      }
    }
    return latency;
  }

  private Duration runBrowseAsset() throws IOException {
    checkArgument(dataset != null, "Missing parameter: dataset");
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create()).build();
    try (DataExploreServiceClient client = new DataExploreServiceClient(getBuilder().build(),
        channel)) {
      Duration latency = Duration.ZERO;
      for (int i = 0; i < runs; i++) {
        Instant start = Instant.now();
        BrowseResponse response = client.browse(dataset, getNamedTable(namedTable));
        latency = Duration.between(start, Instant.now());

        if (loadTest) {
          Statistics stats =
              Statistics.getBuilder(response.getSerializedSize(), latency.toMillis()).build();
          LoadTester.writeStats(stats);
        } else if (generate) {
          String dataFileName = getDataFileName(mode, "browseAsset");
          generateDataFile(dataFileName, response);
        } else {
          for (int index = 0; index < response.getAssetsCount(); index++) {
            BrowseAsset asset = response.getAssets(index);
            System.out.println(asset.getDisplayName() + ":" + asset.getDataset());
          }
        }
      }
      return latency;
    }
  }

  private Duration runResolveSchema() throws IOException {
    checkArgument(dataset != null, "Missing parameter: dataset");
    checkArgument((namedTable != null || nativeQuery != null),
        "Missing parameter: namedTable or nativeQuery");
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create()).build();
    try (ConnectorServiceClient client =
             new ConnectorServiceClient(channel, datasource, getBuilder().build())) {
      Duration latency = Duration.ZERO;
      for (int i = 0; i < runs; i++) {
        Instant start = Instant.now();
        Schema schema =
            client.resolveSchema(dataset, getNamedTable(namedTable), nativeQuery);
        latency = Duration.between(start, Instant.now());

        if (loadTest) {
          Statistics stats =
              Statistics.getBuilder(schema.getSerializedSize(), latency.toMillis()).build();
          LoadTester.writeStats(stats);
        } else if (generate) {
          String dataFileName = getDataFileName(mode, "resolveSchema");
          generateDataFile(dataFileName, schema);
        } else {
          System.out.println(schema);
        }
      }
      return latency;
    }
  }

  private Duration runExecuteQuery() throws IOException {
    checkArgument(dataset != null, "Missing parameter: dataset");
    checkArgument((namedTable != null || nativeQuery != null),
        "Missing parameter: namedTable or nativeQuery");
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create()).build();
    try (ConnectorServiceClient client =
             new ConnectorServiceClient(channel, datasource, getBuilder().build())) {
      Duration totalTime = Duration.ZERO;
      for (int i = 0; i < runs; i++) {
        Data.Builder builder = Data.newBuilder();
        // For load test
        boolean firstBatch = true;
        long totalRows = 0;
        double totalSize = 0;
        double firstBatchSize = 0;
        Duration latency = Duration.ZERO;

        Instant start = Instant.now();
        Iterator<Data> iterator = client.executeQuery(dataset,
            getNamedTable(namedTable), nativeQuery);

        while (iterator.hasNext()) {
          Data data = iterator.next();
          if (loadTest) {
            totalRows += data.getDataBlock().getRowset().getRowsCount();
            totalSize += data.getDataBlock().getSerializedSize();
            if (firstBatch) {
              firstBatchSize = data.getDataBlock().getSerializedSize();
              latency = Duration.between(start, Instant.now());
              firstBatch = false;
            }
          } else if (generate) {
            builder.mergeFrom(data);
          } else {
            System.out.println(data.toString());
          }
        }
        totalTime = Duration.between(start, Instant.now());

        if (loadTest) {
          Statistics stats =
              Statistics.getBuilder(firstBatchSize, latency.toMillis()).setTotalDataSize(totalSize)
                  .setTotalTime(totalTime.toMillis()).setRowsRead(totalRows).build();
          LoadTester.writeStats(stats);
        } else if (generate) {
          Data executeQueryData = builder.build();
          String dataFileName = getDataFileName(mode, "executeQuery");
          generateDataFile(dataFileName, executeQueryData);
        }
      }
      return totalTime;
    }
  }

  private Duration runPrepareQuery() throws IOException {
    checkArgument(dataset != null, "Missing parameter: dataset");
    checkArgument((namedTable != null || nativeQuery != null),
        "Missing parameter: namedTable or nativeQuery");
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create()).build();
    try (ConnectorServiceClient client =
             new ConnectorServiceClient(channel, datasource, getBuilder().build())) {
      Duration latency = Duration.ZERO;
      for (int i = 0; i < runs; i++) {
        Instant start = Instant.now();
        StartQueryResponse response = client.startQuery(dataset,
            getNamedTable(namedTable), nativeQuery);
        latency = Duration.between(start, Instant.now());

        if (loadTest) {
          Statistics stats =
              Statistics.getBuilder(response.getSerializedSize(), latency.toMillis()).build();
          LoadTester.writeStats(stats);
        } else {
          printStartQueryResponse(response);
        }
      }
      return latency;
    }
  }

  private Duration runReadPartition() throws IOException {
    checkArgument(dataset != null, "Missing parameter: dataset");
    checkArgument(stream != null, "Missing parameter: stream");
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create())
        .maxInboundMetadataSize(Integer.MAX_VALUE)  // setting allowed header size to max
        .build();
    Duration totalTime = Duration.ZERO;
    try (ReaderServiceClient client = new ReaderServiceClient(channel, getBuilder().build())) {
      for (int i = 0; i < runs; i++) {
        Data.Builder builder = Data.newBuilder();
        // For load test
        boolean firstBatch = true;
        long totalRows = 0;
        double totalSize = 0;
        double firstBatchSize = 0;
        Duration latency = Duration.ZERO;

        Instant start = Instant.now();
        Iterator<Data> iterator = client.readStream(stream);

        while (iterator.hasNext()) {
          Data data = iterator.next();
          if (loadTest) {
            totalRows += data.getDataBlock().getRowset().getRowsCount();
            totalSize += data.getDataBlock().getSerializedSize();
            if (firstBatch) {
              firstBatchSize = data.getDataBlock().getSerializedSize();
              latency = Duration.between(start, Instant.now());
              firstBatch = false;
            }
          } else if (generate) {
            builder.mergeFrom(data);
          } else {
            System.out.println(data.toString());
          }
        }
        totalTime = Duration.between(start, Instant.now());

        if (loadTest) {
          Statistics stats =
              Statistics.getBuilder(firstBatchSize, latency.toMillis()).setTotalDataSize(totalSize)
                  .setTotalTime(totalTime.toMillis()).setRowsRead(totalRows).build();
          LoadTester.writeStats(stats);
        } else if (generate) {
          Data executeQueryData = builder.build();
          String dataFileName = getDataFileName(mode, "executeQuery");
          generateDataFile(dataFileName, executeQueryData);
        }
      }
      return totalTime;
    }
  }

  /**
   * Generates AsyncQuery Data file when mode is set.
   */
  private Duration runAsyncQuery() throws IOException {
    checkArgument(!loadTest, "Load testing not supported for AsyncQuery");
    checkArgument(dataset != null, "Missing parameter: dataset");
    checkArgument((namedTable != null || nativeQuery != null),
        "Missing parameter: namedTable or nativeQuery");
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create()).build();
    try (ConnectorServiceClient client =
             new ConnectorServiceClient(channel, datasource, getBuilder().build())) {
      Instant start = Instant.now();
      StartQueryResponse response = client.startQuery(dataset,
          getNamedTable(namedTable), nativeQuery);
      try (ReaderServiceClient reader = new ReaderServiceClient(channel, getBuilder().build())) {
        for (ResultSet rs : response.getResultSetsList()) {
          for (int splitIndex = 0; splitIndex < rs.getStreamsCount(); splitIndex++) {
            Data.Builder builder = Data.newBuilder();
            Stream s = rs.getStreams(splitIndex);
            Iterator<Data> iterator = reader.readStream(s.getName());
            while (iterator.hasNext()) {
              Data data = iterator.next();
              if (generate) {
                builder.mergeFrom(data);
              } else {
                System.out.println(data.getDataBlock());
              }
            }
            if (generate) {
              Data asyncQueryData = builder.build();
              String dataFileName = getDataFileName(mode, "asyncQuery_" + splitIndex);
              generateDataFile(dataFileName, asyncQueryData);
            }
          }
        }
        return Duration.between(start, Instant.now());
      }
    }
  }

  private Duration runReadRandomPartition() throws IOException {
    checkArgument(loadTest, "Only load testing supported for ReadRandomPartition");
    checkArgument(dataset != null, "Missing parameter: dataset");
    checkArgument((namedTable != null || nativeQuery != null),
        "Missing parameter: namedTable or nativeQuery");
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create()).build();
    try (ConnectorServiceClient client =
             new ConnectorServiceClient(channel, datasource, getBuilder().build())) {
      StartQueryResponse response = client.startQuery(dataset,
          getNamedTable(namedTable), nativeQuery);
      List<String> streams = new ArrayList<>();
      response
          .getResultSetsList()
          .forEach(r -> r.getStreamsList().forEach(s -> streams.add(s.getName())));

      try (ReaderServiceClient reader = new ReaderServiceClient(channel, getBuilder().build())) {
        Duration totalTime = Duration.ZERO;
        for (int i = 0; i < runs; i++) {
          // For load test
          boolean firstBatch = true;
          long totalRows = 0;
          double totalSize = 0;
          double firstBatchSize = 0;
          Duration latency = Duration.ZERO;

          int randomIndex = (int) (Math.random() * streams.size());
          stream = streams.get(randomIndex);

          Instant start = Instant.now();
          Iterator<Data> iterator = reader.readStream(stream);
          while (iterator.hasNext()) {
            Data data = iterator.next();
            totalRows += data.getDataBlock().getRowset().getRowsCount();
            totalSize += data.getDataBlock().getSerializedSize();
            if (firstBatch) {
              firstBatchSize = data.getDataBlock().getSerializedSize();
              latency = Duration.between(start, Instant.now());
              firstBatch = false;
            }
          }
          totalTime = Duration.between(start, Instant.now());
          Statistics stats =
              Statistics.getBuilder(firstBatchSize, latency.toMillis()).setTotalDataSize(totalSize)
                  .setTotalTime(totalTime.toMillis()).setRowsRead(totalRows).build();
          LoadTester.writeStats(stats);
        }
        return totalTime;
      }
    }
  }

  private DataSource getDatasource() {
    ManagedChannel channel = Grpc.newChannelBuilder(getTargetString(),
        InsecureChannelCredentials.create()).build();
    try (ConnectorServiceClient client =
             new ConnectorServiceClient(channel, datasource, Struct.newBuilder().build())) {
      return client.getDataSource();
    }
  }

  private Struct.Builder getBuilder() throws IOException {
    if (builder != null) {
      return builder;
    }

    Properties props = new Properties();
    props.load(new StringReader(configuration.replaceAll(",", "\n")));

    DataSource dataSource = getDatasource();
    builder = Struct.newBuilder();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      Value.Builder vbuilder = getValueBuilder(entry, dataSource.getParametersList());
      builder.putFields((String) entry.getKey(), vbuilder.build());
    }

    return builder;
  }

  private Value.Builder getValueBuilder(Map.Entry<Object, Object> conf,
                                        List<Parameter> parameters) {
    try {
      Parameter parameter = parameters
          .stream()
          .filter(param -> conf.getKey().equals(param.getId()))
          .findFirst().get();

      Object value = conf.getValue();
      return switch (parameter.getType().getTypeKind()) {
        case INT32 -> Value.newBuilder().setNumberValue(Double.parseDouble((String) value));
        case BOOL -> Value.newBuilder().setBoolValue(Boolean.parseBoolean((String) value));
        default -> Value.newBuilder().setStringValue(String.valueOf(value)); // Default is String
      };
    } catch (NoSuchElementException ex) {
      throw new RuntimeException(String.format("'%s' not found in the configuration %s.json.",
          conf.getKey(), datasource), ex);
    }
  }

  private String getTargetString() {
    return String.format("%s:%s", server, port);
  }

  private String getDataFileName(String mode, String methodName) {
    return TEST_RESOURCES + datasource + "_" + mode + "_" + methodName + "_data.pb";
  }

  private <T extends com.google.protobuf.GeneratedMessageV3> void generateDataFile(
      String dataFileName,
      T expectedData) throws IOException {
    FileOutputStream fileOut = new FileOutputStream(dataFileName);
    expectedData.writeTo(fileOut);
    fileOut.close();
  }

  private static void printStartQueryResponse(StartQueryResponse response) {
    System.out.println("Total partitions returned: " + response.getResultSets(0).getStreamsCount());
    System.out.println("Maximum Concurrency: " + response.getMaxConcurrency());
    for (int i = 0; i < response.getResultSetsCount(); i++) {
      System.out.printf("******RESULT-%d******%n", i);
      System.out.println(response.getResultSets(i));
    }
  }

  @Nullable
  private static NamedTable getNamedTable(String namedTable) {
    return isNullOrWhiteSpace(namedTable) ? NamedTable.getDefaultInstance()
        : NamedTable.newBuilder().addAllNames(
            Arrays.stream(namedTable.split("/")).toList()).build();
  }

  private static boolean isNullOrWhiteSpace(String namedTable) {
    return namedTable == null || namedTable.isEmpty() || namedTable.trim().isEmpty();
  }
}
