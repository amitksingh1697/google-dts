package com.google.cloud.connector.api;

import static com.google.cloud.connector.api.DatasetName.COLLECTION_ID_DATASOURCE;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DatasetName}.
 */
public class DatasetNameTest {
  private static final String DATASOURCE = "datasource";
  private static final DatasetName.Component DATASOURCE_COMPONENT =
      new DatasetName.Component(COLLECTION_ID_DATASOURCE, DATASOURCE);

  private static final String COLLECTION_ID_INSTANCE = "instances";
  private static final String INSTANCE = "instance";
  private static final DatasetName.Component INSTANCE_COMPONENT =
      new DatasetName.Component(COLLECTION_ID_INSTANCE, INSTANCE);

  private static final List<DatasetName.Component> COMPONENTS =
      List.of(DATASOURCE_COMPONENT, INSTANCE_COMPONENT);
  private static final String DATASET_NAME = String.format("%s/%s/%s/%s",
      COLLECTION_ID_DATASOURCE, DATASOURCE, COLLECTION_ID_INSTANCE, INSTANCE);

  @Test
  public void fromComponents_returnsValidDataset() {
    DatasetName datasetName = DatasetName.fromComponents(COMPONENTS);

    assertThat(datasetName.name()).isEqualTo(DATASET_NAME);
    assertThat(datasetName.componentCount()).isEqualTo(2);
    assertThat(datasetName.components()).containsExactlyElementsIn(COMPONENTS).inOrder();
    assertThat(datasetName.hasComponent(COLLECTION_ID_INSTANCE)).isTrue();
    assertThat(datasetName.hasComponent("random")).isFalse();
  }

  @Test
  public void fromComponentsWithoutDatasource_fails() {
    assertThrows(IllegalArgumentException.class,
        () -> DatasetName.fromComponents(List.of(INSTANCE_COMPONENT)));
  }

  @Test
  public void fromName_returnsValidDataset() {
    DatasetName datasetName = DatasetName.fromName(DATASET_NAME);

    assertThat(datasetName.name()).isEqualTo(DATASET_NAME);
    assertThat(datasetName.componentCount()).isEqualTo(2);
    assertThat(datasetName.components()).containsExactlyElementsIn(COMPONENTS).inOrder();
    assertThat(datasetName.hasComponent(COLLECTION_ID_INSTANCE)).isTrue();
  }

  @Test
  public void fromNameWithoutDatasource_fails() {
    assertThrows(IllegalArgumentException.class,
        () -> DatasetName.fromName(String.format("%s/%s", COLLECTION_ID_INSTANCE, INSTANCE)));
  }

  @Test
  public void fromInvalidName_fails() {
    assertThrows(IllegalArgumentException.class,
        () -> DatasetName.fromName(String.format(COLLECTION_ID_DATASOURCE)));
  }

  @Test
  public void fromEmptyName_fails() {
    assertThrows(IllegalArgumentException.class, () -> DatasetName.fromName(""));
  }

  @Test
  public void appendComponent_returnsValidDataset() {
    DatasetName datasetName = DatasetName.fromComponents(List.of(DATASOURCE_COMPONENT));

    DatasetName appendedDatasetName = datasetName.append(INSTANCE_COMPONENT);

    assertThat(appendedDatasetName.name()).isEqualTo(DATASET_NAME);
    assertThat(appendedDatasetName.components()).containsExactlyElementsIn(COMPONENTS).inOrder();
  }

  @Test
  public void appendDuplicateComponent_fails() {
    DatasetName datasetName = DatasetName.fromComponents(List.of(DATASOURCE_COMPONENT));

    assertThrows(IllegalArgumentException.class, () -> datasetName.append(DATASOURCE_COMPONENT));
  }
}
