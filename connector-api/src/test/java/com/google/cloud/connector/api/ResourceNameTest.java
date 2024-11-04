package com.google.cloud.connector.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.cloud.connector.api.ResourceName.Component;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ResourceName}. */
public class ResourceNameTest {

  private static final String COLLECTION_ID_DATASOURCE = "datasources";
  private static final String RESOURCE_ID_DATASOURCE = "some_ds";
  private static final String COLLECTION_ID_INSTANCES = "instances";
  private static final String RESOURCE_ID_INSTANCES = "some_instance";
  private static final String COLLECTION_ID_DATASETS = "datasets";
  private static final String RESOURCE_ID_DATASETS = "some_ds";

  private static final String RESOURCE_NAME =
      String.join(
          "/",
          ImmutableList.of(
              COLLECTION_ID_DATASOURCE, RESOURCE_ID_DATASOURCE,
              COLLECTION_ID_INSTANCES, RESOURCE_ID_INSTANCES,
              COLLECTION_ID_DATASETS, RESOURCE_ID_DATASETS));

  @Test
  public void createResourceNameFromString_returnValidObject() {
    ResourceName assetName = ResourceName.fromName(RESOURCE_NAME);

    assertThat(assetName.name()).isEqualTo(RESOURCE_NAME);
    List<Component> componentList = assetName.components();

    assertThat(componentList)
        .containsExactly(
            new Component(COLLECTION_ID_DATASOURCE, RESOURCE_ID_DATASOURCE),
            new Component(COLLECTION_ID_INSTANCES, RESOURCE_ID_INSTANCES),
            new Component(COLLECTION_ID_DATASETS, RESOURCE_ID_DATASETS))
        .inOrder();
  }

  @Test
  public void createResourceNameFromNullString_throwsException() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              ResourceName unused = ResourceName.fromName(null);
            });
    assertThat(exception.getMessage()).contains("should be non-null");
  }

  @Test
  public void createResourceNameFromEmptyString_throwsException() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              ResourceName unused = ResourceName.fromName("");
            });
    assertThat(exception.getMessage()).contains("should be non-empty");
  }

  @Test
  public void createResourceNameFromComponents_returnValidObject() {
    List<Component> components = new ArrayList<>();
    components.add(new Component(COLLECTION_ID_DATASOURCE, RESOURCE_ID_DATASOURCE));
    components.add(new Component(COLLECTION_ID_INSTANCES, RESOURCE_ID_INSTANCES));
    components.add(new Component(COLLECTION_ID_DATASETS, RESOURCE_ID_DATASETS));

    ResourceName assetName = ResourceName.fromComponents(components);

    assertThat(assetName.name()).isEqualTo(RESOURCE_NAME);
    assertThat(assetName.components()).containsExactlyElementsIn(components).inOrder();
  }

  @Test
  public void createResourceNameFromNullComponents_throwsException() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              ResourceName unused = ResourceName.fromComponents(null);
            });
    assertThat(exception.getMessage()).contains("should be non-null");
  }

  @Test
  public void createResourceNameFromEmptyComponents_throwsException() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              ResourceName unused = ResourceName.fromComponents(Collections.emptyList());
            });

    assertThat(exception.getMessage()).contains("should be non-empty");
  }

  @Test
  public void asMap() {
    ResourceName assetName = ResourceName.fromName(RESOURCE_NAME);

    assertThat(assetName.asMap())
        .containsExactly(
            COLLECTION_ID_DATASOURCE,
            RESOURCE_ID_DATASOURCE,
            COLLECTION_ID_INSTANCES,
            RESOURCE_ID_INSTANCES,
            COLLECTION_ID_DATASETS,
            RESOURCE_ID_DATASETS)
        .inOrder();
  }

  @Test
  public void createComponent_throwsExceptionOnInvalidCollectionId() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Component unused = new Component("invalid", "anyvalue");
            });
    assertThat(exception.getMessage())
        .isEqualTo("Collection ID should be 'datasources', 'instances' or 'datasets'");
  }

  @Test
  void getDatasource_returnsDatasource() {
    ResourceName resourceName = ResourceName.fromName(RESOURCE_NAME);
    assertThat(resourceName.getDatasource()).isEqualTo(RESOURCE_ID_DATASOURCE);
  }

  @Test
  void getDatasource_returnsNullDatasourceIfNotSet() {
    ResourceName resourceName = ResourceName.ROOT_RESOURCE;
    assertThat(resourceName.getDatasource()).isNull();
  }

  @Test
  void getInstance() {
    ResourceName resourceName = ResourceName.fromName(RESOURCE_NAME);
    assertThat(resourceName.getInstance()).isEqualTo(RESOURCE_ID_INSTANCES);
  }

  @Test
  void getInstance_returnsNullDatasourceIfNotSet() {
    ResourceName resourceName = ResourceName.ROOT_RESOURCE;
    assertThat(resourceName.getInstance()).isNull();
  }

  @Test
  void getDataset() {
    ResourceName resourceName = ResourceName.fromName(RESOURCE_NAME);
    assertThat(resourceName.getDataset()).isEqualTo(RESOURCE_ID_DATASETS);
  }

  @Test
  void getDataset_returnsNullDatasourceIfNotSet() {
    ResourceName resourceName = ResourceName.ROOT_RESOURCE;
    assertThat(resourceName.getDataset()).isNull();
  }
}
