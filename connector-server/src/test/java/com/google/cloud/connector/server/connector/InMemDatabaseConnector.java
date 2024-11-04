package com.google.cloud.connector.server.connector;

import com.google.cloud.bigquery.federation.v1alpha1.DataQuery;
import com.google.cloud.bigquery.federation.v1alpha1.NamedTable;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.BrowseAsset;
import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.api.DataExplorer;
import com.google.cloud.connector.api.NativeQuerySchemaResolver;
import com.google.cloud.connector.api.ParallelQueryExecutor;
import com.google.cloud.connector.api.ParallelQueryPreparationContext;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.browse.BrowseAssetResult;
import com.google.cloud.connector.api.data.RecordBuilder;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/** A connector for a simple in-memory database * */
public class InMemDatabaseConnector
    implements SynchronousQueryExecutor,
        Connector,
        NativeQuerySchemaResolver,
        ParallelQueryExecutor,
        DataExplorer {
  public static final String DATASOURCE_ID = "inmemdb";
  public static final String DATABASE_NAME = "inMemDatabase";

  public static final String CONFIG_KEY_NUM_ROWS = "numRows";

  public static final String TABLE_NAME_PERSON = "person";
  public static final String TABLE_NAME_CUSTOMER = "customer";

  public static final int QUERY_MAX_CONCURRENCY = 1;

  public static final BrowseAsset DATABASE_ASSET =
      BrowseAsset.newBuilder()
          .setNamedTable(NamedTable.newBuilder().addNames(DATABASE_NAME))
          .setDisplayName(DATABASE_NAME)
          .build();

  public static final BrowseAsset TABLE_ASSET_PERSON =
      BrowseAsset.newBuilder()
          .setNamedTable(
              NamedTable.newBuilder().addNames(DATABASE_NAME).addNames(TABLE_NAME_PERSON))
          .setDisplayName(TABLE_NAME_PERSON)
          .setLeafLevel(true)
          .build();

  public static final BrowseAsset TABLE_ASSET_CUSTOMER =
      BrowseAsset.newBuilder()
          .setNamedTable(
              NamedTable.newBuilder().addNames(DATABASE_NAME).addNames(TABLE_NAME_CUSTOMER))
          .setDisplayName(TABLE_NAME_CUSTOMER)
          .setLeafLevel(true)
          .build();

  /**
   * Connector config that defines the parameter this connector requires.
   *
   * @param numRows the number of dummy data rows each table should have.
   */
  public record Config(int numRows) {}

  private final Config config;
  private final Database database;

  @DataSource(value = DATASOURCE_ID)
  public InMemDatabaseConnector(Config config) {
    this.config = config;

    final List<Table> tables = new ArrayList<>();

    Map<String, Class<?>> personTableSchema = new HashMap<>();
    personTableSchema.put("firstName", String.class);
    personTableSchema.put("lastName", String.class);
    personTableSchema.put("age", Integer.class);
    tables.add(new Table(TABLE_NAME_PERSON, personTableSchema, config.numRows()));

    Map<String, Class<?>> customerTableSchema = new HashMap<>();
    customerTableSchema.put("id", String.class);
    customerTableSchema.put("userName", String.class);
    tables.add(new Table(TABLE_NAME_CUSTOMER, customerTableSchema, config.numRows()));

    this.database = new Database(DATABASE_NAME, tables);
  }

  @Override
  public void resolveSchema(AssetName assetName, SchemaBuilder schemaBuilder) {
    if (assetName.getNumElements() < 1) {
      throw new RuntimeException(
          String.format(
              "Asset name should have at least 1 element, but got '%s'",
              assetName.name()));
    }
    Table table = database.getTable(assetName.getElement(1));

    Map<String, Class<?>> tableSchema = table.getSchema();

    Table.exportSchema(schemaBuilder, table.getName(), tableSchema);
  }

  @Override
  public void resolveSchema(AssetName assetName, String nativeQuery, SchemaBuilder schemaBuilder) {
    var nativeQueryMatcher = Pattern.compile("(?i)SELECT \\* FROM (.+)").matcher(nativeQuery);
    if (!nativeQueryMatcher.matches()) {
      throw new RuntimeException(
          String.format(
              "Native query must be in the form 'SELECT * FROM <table>' but got: %s", nativeQuery));
    }
    Table table = database.getTable(nativeQueryMatcher.group(1));

    Map<String, Class<?>> tableSchema = table.getSchema();

    Table.exportSchema(schemaBuilder, table.getName(), tableSchema);
  }

  @Override
  public BrowseAssetResult browseAsset(AssetName parentAssetName) {
    if (parentAssetName.getNumElements() > 1) {
      throw new RuntimeException(
          String.format(
              "Asset name should be empty to browse from root level, or in the format of "
                  + "databases/{} to browse tables, but got '%s'",
              parentAssetName));
    }

    // This means the request wants to browse from root level
    if (parentAssetName.getNumElements() == 0) {
      return new SimpleBrowseAssetResult(Collections.singletonList(DATABASE_ASSET));
    }

    // Browse the database
    if (!parentAssetName.getElement(0).equals(DATABASE_NAME)) {
      throw new RuntimeException(
          String.format(
              "Unknown asset in the data source, the supported asset is %s, but got '%s'",
              DATABASE_ASSET.getDataset(), parentAssetName.name()));
    }

    return new SimpleBrowseAssetResult(
        Collections.unmodifiableList(Arrays.asList(TABLE_ASSET_PERSON, TABLE_ASSET_CUSTOMER)));
  }

  @Override
  public RecordReader execute(AssetName assetName, DataQuery dataQuery) throws IOException {
    if (assetName.getNumElements() < 1) {
      throw new RuntimeException(
          String.format("Asset name should have 1 element, but got '%s'", assetName));
    }

    String tableName = getTableName(dataQuery);

    return new TableRecordReader(database.getTable(tableName));
  }

  private String getTableName(DataQuery dataQuery) {
    if (dataQuery.hasNamedTable()) {
      return dataQuery.getNamedTable().getNames(1);
    }
    if (dataQuery.hasTable()) {
      return dataQuery.getTable();
    }
    throw new RuntimeException(
        String.format(
            "Table and NamedTable not present in the data query: '%s'.",
            TextFormat.shortDebugString(dataQuery)));
  }

  @Override
  public void prepareQuery(
      AssetName assetName, DataQuery query, ParallelQueryPreparationContext context) {
    if (assetName.getNumElements() < 1) {
      throw new RuntimeException(
          String.format("Asset name should have 1 element, but got '%s'", assetName));
    }

    if (!query.hasNamedTable()) {
      throw new RuntimeException(
          String.format(
              "Only named table querying is supported. Named table is missing in"
                  + " the query for asset %s:"
                  + " %s",
              assetName, TextFormat.shortDebugString(query)));
    }

    context.setMaxConcurrency(QUERY_MAX_CONCURRENCY);
    context.addPartition(query.getNamedTable().getNames(1).getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public RecordReader readPartition(AssetName assetName, byte[] partitionData) throws IOException {
    String tableName = new String(partitionData, StandardCharsets.UTF_8);

    return new TableRecordReader(database.getTable(tableName));
  }

  @Override
  public String toString() {
    return String.format(
        "ConnectorName: '%s'. ConnectorConfig: numRows=%d", getClass().getName(), config.numRows);
  }

  /** An in-memory database containing a few tables. */
  static class Database {
    private final String name;

    /** A map from {@link Table#name} to {@link Table} */
    private final Map<String, Table> tables;

    Database(String name, List<Table> tables) {
      this.name = name;
      this.tables = new HashMap<>();
      for (Table table : tables) {
        this.tables.put(table.getName(), table);
      }
    }

    public String getName() {
      return name;
    }

    public Table getTable(String name) {
      return tables.get(name);
    }
  }

  /** An in-memory table that may store some records. */
  static class Table implements Iterable<Map<String, Object>> {
    private static final int RAND_STRING_MAX_LENGTH = 10;
    private static final int RAND_INT_MAX_VALUE = 100;
    private final char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private final Random random = new Random();
    private final String name;

    /** A map from field name to its type. */
    private final Map<String, Class<?>> schema;

    /**
     * A map from row key to a row record. Each row record is represented by a map from field name
     * to value.
     */
    private final Map<String, Map<String, Object>> rows;

    Table(String name, Map<String, Class<?>> schema, int numRows) {
      this.name = name;
      this.schema = schema;
      this.rows = new HashMap<>();
      for (int i = 0; i < numRows; i++) {
        rows.put(String.valueOf(i), generateOneRow(schema));
      }
    }

    public static void exportSchema(
        SchemaBuilder schemaBuilder, String tableName, Map<String, Class<?>> tableSchema) {
      schemaBuilder.name(tableName);
      for (Map.Entry<String, Class<?>> entry : tableSchema.entrySet()) {
        Class<?> classz = entry.getValue();
        if (classz.equals(String.class)) {
          schemaBuilder.field(entry.getKey()).typeString();
        } else if (classz.equals(Integer.class)) {
          schemaBuilder.field(entry.getKey()).typeInt64();
        } else {
          throw new RuntimeException(
              String.format("Unsupported field type '%s'", classz.getName()));
        }
      }
      schemaBuilder.endStruct();
    }

    public String getName() {
      return name;
    }

    public void insertRow(String key, Map<String, Object> row) {
      rows.put(key, row);
    }

    public Map<String, Object> getRow(String key) {
      return rows.get(key);
    }

    public Map<String, Class<?>> getSchema() {
      return this.schema;
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
      return rows.values().iterator();
    }

    Map<String, Object> generateOneRow(Map<String, Class<?>> schema) {
      Map<String, Object> row = new HashMap<>();
      for (Map.Entry<String, Class<?>> entry : schema.entrySet()) {
        Class<?> classz = entry.getValue();
        if (classz.equals(String.class)) {
          row.put(entry.getKey(), generateRandomString());
        } else if (classz.equals(Integer.class)) {
          row.put(entry.getKey(), generateRandomInteger());
        } else {
          throw new RuntimeException(
              String.format("Unsupported field type '%s'", classz.getName()));
        }
      }
      return row;
    }

    private int generateRandomInteger() {
      return random.nextInt(RAND_INT_MAX_VALUE);
    }

    private String generateRandomString() {
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < RAND_STRING_MAX_LENGTH; i++) {
        char c = chars[random.nextInt(chars.length)];
        stringBuilder.append(c);
      }
      return stringBuilder.toString();
    }
  }

  static class TableRecordReader implements RecordReader {
    private final Table table;
    private final Iterator<Map<String, Object>> iterator;

    TableRecordReader(Table table) {
      this.table = table;
      this.iterator = table.iterator();
    }

    @Override
    public void getRecordSchema(SchemaBuilder schemaBuilder) {
      Table.exportSchema(schemaBuilder, table.getName(), table.getSchema());
    }

    @Override
    public boolean nextRecord(RecordBuilder recordBuilder) throws IOException {
      if (!iterator.hasNext()) {
        return false;
      }
      Map<String, Object> row = iterator.next();
      for (Map.Entry<String, Object> cell : row.entrySet()) {
        Class<?> classz = cell.getValue().getClass();
        if (classz.equals(String.class)) {
          recordBuilder.field(cell.getKey()).set((String) cell.getValue());
        } else if (classz.equals(Integer.class)) {
          recordBuilder.field(cell.getKey()).set((Integer) cell.getValue());
        } else {
          throw new RuntimeException(
              String.format("Unsupported field type '%s'", classz.getName()));
        }
      }
      recordBuilder.endStruct();

      return true;
    }

    @Override
    public void close() throws IOException {}
  }

  /** A simple browse asset result to return assets based on the given list of assets. */
  static class SimpleBrowseAssetResult implements BrowseAssetResult {

    private final List<BrowseAsset> assets;

    SimpleBrowseAssetResult(List<BrowseAsset> assets) {
      this.assets = assets;
    }

    @Override
    public Iterator<BrowseAsset> iterator() {
      return new Iterator<>() {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
          return currentIndex < assets.size();
        }

        @Override
        public BrowseAsset next() {
          return assets.get(currentIndex++);
        }
      };
    }
  }
}