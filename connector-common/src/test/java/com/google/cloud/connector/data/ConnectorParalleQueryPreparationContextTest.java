package com.google.cloud.connector.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse;
import com.google.cloud.connector.api.AssetName;
import com.google.cloud.connector.api.DatasetName;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

public class ConnectorParalleQueryPreparationContextTest {
  private static final Gson GSON = new Gson();

  @Test
  public void addPartition_IsSuccessful_ForValidPartitionData() {
    final String dummyString = "DummyString=";
    final String dataset = "datasources/datasource/instances/instance";
    final String expectedResultSetName =
        "datasources/datasource/instances/instance/resultsets/single-resultset";
    final String expectedStreamId = createTestPartitionEncodedString(dummyString.getBytes(), null);
    DatasetName datasetName = DatasetName.fromName(dataset);
    ConnectorParallelQueryPreparationContext context =
        new ConnectorParallelQueryPreparationContext(datasetName, null);
    context.addPartition(dummyString.getBytes());

    StartQueryResponse response = context.buildStartQueryResponse();

    assertNotNull(response);
    assertNotNull(response.getResultSets(0));
    StartQueryResponse.ResultSet actualResultSet = response.getResultSets(0);
    assertEquals(expectedResultSetName, actualResultSet.getName());
    assertEquals(1, response.getResultSets(0).getStreamsCount());

    assertNotNull(actualResultSet.getStreams(0));
    StartQueryResponse.ResultSet.Stream actualStream = actualResultSet.getStreams(0);
    assertEquals(expectedStreamId, actualStream.getId());
  }

  @Test
  public void buildResponse_ReturnsAssetInfoForNoQueryPartition() {
    final String dataset = "datasources/datasource/instances/instance";
    final AssetInfo testAsset = new AssetInfo(
        new AssetName("test-asset", List.of("test-asset")), null);
    final String expectedStreamId = createTestPartitionEncodedString(null, testAsset);
    DatasetName datasetName = DatasetName.fromName(dataset);
    ConnectorParallelQueryPreparationContext context =
        new ConnectorParallelQueryPreparationContext(datasetName, testAsset);

    StartQueryResponse response = context.buildStartQueryResponse();

    assertNotNull(response);
    assertNotNull(response.getResultSets(0));
    assertEquals(response.getResultSets(0).getStreamsCount(), 1);
    assertNotNull(response.getResultSets(0).getStreams(0));
    assertEquals(response.getResultSets(0).getStreams(0).getId(), expectedStreamId);
  }

  @Test
  public void setMaxConcurrency_OverrideNumberOfPartitions() {
    final DatasetName datasetName =
        DatasetName.fromName("datasources/datasource/instances/instance");
    final ConnectorParallelQueryPreparationContext context =
        new ConnectorParallelQueryPreparationContext(datasetName, null);
    String dummyString = "DummyString=";
    final String expectedStreamId = createTestPartitionEncodedString(dummyString.getBytes(), null);
    context.addPartition(dummyString.getBytes());
    context.addPartition(dummyString.getBytes());
    context.setMaxConcurrency(1);

    StartQueryResponse response = context.buildStartQueryResponse();

    assertNotNull(response);
    assertNotNull(response.getResultSets(0));
    assertEquals(response.getResultSets(0).getStreamsCount(), 2);
    assertNotNull(response.getResultSets(0).getStreams(0));
    assertEquals(response.getResultSets(0).getStreams(0).getId(), expectedStreamId);
    assertNotNull(response.getResultSets(0).getStreams(1));
    assertEquals(response.getResultSets(0).getStreams(1).getId(), expectedStreamId);
    assertEquals(response.getMaxConcurrency(), 1);
  }

  private String createTestPartitionEncodedString(byte[] query, @Nullable AssetInfo assetInfo) {
    return Base64.getUrlEncoder().encodeToString(
        GSON.toJson(new Partition(query, assetInfo)).getBytes(StandardCharsets.UTF_8));
  }
}
