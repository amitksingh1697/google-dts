package com.google.cloud.connector.data;

import static com.google.cloud.connector.data.ResultStreamId.COLLECTION_ID_RESULTSET;
import static com.google.cloud.connector.data.ResultStreamId.COLLECTION_ID_STREAM;

import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse.ResultSet;
import com.google.cloud.bigquery.federation.v1alpha1.StartQueryResponse.ResultSet.Stream;
import com.google.cloud.connector.api.DatasetName;
import com.google.cloud.connector.api.ParallelQueryPreparationContext;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

/**
 * Implementation of {@link ParallelQueryPreparationContext}.
 */
public class ConnectorParallelQueryPreparationContext implements ParallelQueryPreparationContext {

  /**
   * Resultset resource ID. At the moment, only one result set is supported with a hard-coded
   * resource id. This ID is currently just a placeholder and not consumed or used anywhere.
   */
  private static final String RESULTSET_ID = "single-resultset";
  private static final DatasetName.Component RESULT_SET_COMPONENT =
      new DatasetName.Component(COLLECTION_ID_RESULTSET, RESULTSET_ID);

  private final DatasetName resultSetName;
  private final ResultSet.Builder resultSetBuilder;
  private final StartQueryResponse.Builder startQueryRespBuilder;
  private final ImmutableList.Builder<Partition> partitionsBuilder;
  private final AssetInfo assetInfo;
  private static final Gson GSON = new Gson();

  /**
   * Constructor.
   *
   * @param datasetName the parent of the {@link DatasetName} this query operates on.
   * @param assetInfo of type {@link AssetInfo} to store info about asset
   *                  on which StartQuery is called
   */
  public ConnectorParallelQueryPreparationContext(
      DatasetName datasetName, @Nullable AssetInfo assetInfo) {
    this.resultSetName = datasetName.append(RESULT_SET_COMPONENT);
    this.resultSetBuilder = ResultSet.newBuilder().setName(resultSetName.name());
    this.startQueryRespBuilder = StartQueryResponse.newBuilder();
    this.partitionsBuilder = ImmutableList.builder();
    this.assetInfo = assetInfo;
  }

  @Override
  public void addPartition(byte[] partitionData) {
    partitionsBuilder.add(new Partition(partitionData, null));
  }

  @Override
  public void setMaxConcurrency(int maxConcurrency) {
    startQueryRespBuilder.setMaxConcurrency(maxConcurrency);
  }

  /**
   * Returns a {@link StartQueryResponse} that is produced from a list of added partitions.
   *
   * @return a {@link StartQueryResponse} that can be used to initiate stream read later.
   */
  public StartQueryResponse buildStartQueryResponse() {
    ImmutableList<Partition> partitions = partitionsBuilder.build();
    if (partitions.isEmpty()) {
      partitions = ImmutableList.of(new Partition(null, assetInfo));
    }
    for (Partition partition : partitions) {
      String encodedString = ResultStreamId.encode(
          GSON.toJson(partition).getBytes(StandardCharsets.UTF_8));
      DatasetName.Component streamComponent =
          new DatasetName.Component(COLLECTION_ID_STREAM, encodedString);
      DatasetName streamName = resultSetName.append(streamComponent);
      resultSetBuilder.addStreams(
          Stream.newBuilder().setName(streamName.name()).setId(encodedString).build());
    }
    return startQueryRespBuilder.addResultSets(resultSetBuilder.build()).build();
  }
}
