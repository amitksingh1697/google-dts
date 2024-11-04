package com.google.cloud.connector.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the dataset that the query is targeted to. It has components,
 * which are key value pairs of (collectionId, resourceId), typically in the format: <br>
 * <code>datasources/{datasource}/instances/{instance}/...</code> <br>
 * Only the "datasources" component is compulsory, all other components are optional. The dataset
 * must begin with the "datasources" component.
 *
 * @param name       Name of the dataset, in the form of "key1/value1/key2/value2/..."
 * @param components list of {@link Component} entries used to create this {@link DatasetName}
 */
public record DatasetName(String name, String datasource, List<Component> components) {
  // collectionId for the component that represents a datasource.
  public static final String COLLECTION_ID_DATASOURCE = "datasources";

  /**
   * Canonical constructor.
   */
  public DatasetName(String name, String datasource, List<Component> components) {
    this.name = name;
    this.datasource = datasource;
    this.components = List.copyOf(components);
  }

  /**
   * A {@link Component} represents a {collectionId}/{resourceId} pair that is part of the overall
   * resource name for identifying a specific layer in the hierarchy level.
   */
  public record Component(String collectionId, String resourceId) {
  }

  /**
   * Creates a new {@link DatasetName} with a new component appended.
   *
   * @param component the additional new component to append to {@link DatasetName}
   * @return a new {@link DatasetName} that with the new {@link Component} appended.
   */
  public DatasetName append(Component component) {
    Preconditions.checkArgument(!hasComponent(component.collectionId),
        "The collection Id %s already exists in the dataset name %s",
        component.collectionId(), name);
    List<Component> newComponents =
        ImmutableList.<Component>builder().addAll(components).add(component).build();
    return DatasetName.fromComponents(newComponents);
  }

  /**
   * Creates a {@link DatasetName} from supplied list of components.
   */
  public static DatasetName fromComponents(List<Component> components) {
    Preconditions.checkArgument(components != null && !components.isEmpty(),
        "Dataset component list should not be null or empty.");
    validateDatasourceComponent(components);
    String datasource = components.get(0).resourceId;
    return new DatasetName(createNameFromComponents(components), datasource, components);
  }

  /**
   * Creates a {@link DatasetName} from supplied dataset name.
   */
  public static DatasetName fromName(String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name),
        "Dataset name (string) should not be null or empty.");
    List<Component> components = parseNameToComponents(name);
    validateDatasourceComponent(components);
    String datasource = components.get(0).resourceId;
    return new DatasetName(name, datasource, components);
  }

  /**
   * Returns the total number of components that this {@link DatasetName} is composed of.
   */
  public int componentCount() {
    return components().size();
  }

  /**
   * Checks if a component with given collectionId exists.
   */
  public boolean hasComponent(String collectionId) {
    for (Component component : components) {
      if (component.collectionId.equals(collectionId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the component that has the given collectionId.
   *
   * @throws IllegalArgumentException if component with collectionId does not exist
   */
  public Component getComponent(String collectionId) throws IllegalArgumentException {
    for (Component component : components) {
      if (component.collectionId.equals(collectionId)) {
        return component;
      }
    }
    throw new IllegalArgumentException(
        String.format("Component with collectionId %s does not exist in DatasetName %s",
            collectionId, name));
  }

  @Override
  public String toString() {
    return name;
  }

  private static List<Component> parseNameToComponents(String name) {
    StringTokenizer tokenizer = new StringTokenizer(name, "/");
    Preconditions.checkArgument(tokenizer.countTokens() % 2 == 0,
        "Dataset name should have even number of components separated by '/'");
    List<Component> result = new ArrayList<>();
    while (tokenizer.hasMoreTokens()) {
      result.add(new Component(tokenizer.nextToken(), tokenizer.nextToken()));
    }
    return Collections.unmodifiableList(result);
  }

  private static String createNameFromComponents(List<Component> components) {
    return components.stream()
        .flatMap(component -> Stream.of(component.collectionId(), component.resourceId()))
        .collect(Collectors.joining("/"));
  }

  private static void validateDatasourceComponent(List<Component> components) {
    Preconditions.checkArgument(
        components.get(0).collectionId.equals(COLLECTION_ID_DATASOURCE),
        "Dataset component list does not have a 'datasource' component in the first position.");
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(components.get(0).resourceId),
        "Datasource resourceId in dataset component list should not be null or empty.");
  }
}
