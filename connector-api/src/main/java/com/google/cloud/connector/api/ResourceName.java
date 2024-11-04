package com.google.cloud.connector.api;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * {@link ResourceName} represents a Connector resource name in the form of a URI path without
 * leading "/" (e.g. "datasources/some_ds/instances/some_instance/datasets/some_ds"). This is
 * currently used by the `dataset` field in the Federation V1 API.
 *
 * <p>The resource name is composed of a set of "{collectionId}/{resourceId}" pairs, where
 * "collectionId" identifies the type of resource at a specific level of resource hierarchy and
 * "resourceId" identifies a specific resource at that level.
 */
public record ResourceName(String name, List<Component> components) {
  // This is the asset that represents the root level. At root level, the asset name and components
  // will be empty.
  public static final ResourceName ROOT_RESOURCE = new ResourceName("", Collections.emptyList());
  public static final String DATASOURCES = "datasources";
  public static final String INSTANCES = "instances";
  public static final String DATASETS = "datasets";
  private static final Set<String> ALLOWED_COLLECTION_IDS =
      Set.of(DATASOURCES, INSTANCES, DATASETS);

  /**
   * A {@link Component} represents a {collectionId}/{resourceId} pair that is part of the overall
   * resource name for identifying a specific layer in the hierarchy level.
   */
  public record Component(String collectionId, String resourceId) {

    /**
     * Canonical constructor.
     *
     * @param collectionId collection ID to use.
     * @param resourceId resource ID to use.
     */
    public Component(String collectionId, String resourceId) {
      // Check if the collection ID matches one of the allowed values.
      if (!ALLOWED_COLLECTION_IDS.contains(collectionId)) {
        throw new IllegalArgumentException(
            "Collection ID should be '"
                + DATASOURCES
                + "', '"
                + INSTANCES
                + "' or '"
                + DATASETS
                + "'");
      }

      this.collectionId = collectionId;
      this.resourceId = resourceId;
    }
  }

  public ResourceName(String name, List<Component> components) {
    this.name = name;
    this.components = List.copyOf(components);
  }

  /**
   * Returns the total number of components that this {@link ResourceName} is composed of.
   *
   * @return the total number of components that this {@link ResourceName} is composed of.
   */
  public int componentCount() {
    return components().size();
  }

  /**
   * Returns the components as a {@link Map} keyed by the {@link Component#collectionId} with {@link
   * Component#resourceId()} as the value.
   */
  public Map<String, String> asMap() {
    return components.stream()
        .collect(
            ImmutableMap::<String, String>builder,
            (builder, component) -> builder.put(component.collectionId(), component.resourceId()),
            (left, right) -> {
              throw new UnsupportedOperationException();
            })
        .build();
  }

  /**
   * Get the Datasource name.
   *
   * @return the datasource name, or null if not specified.
   */
  @Nullable
  public String getDatasource() {
    return asMap().get(DATASOURCES);
  }

  /**
   * Get the Instance name.
   *
   * @return the instance name, or null if not specified.
   */
  @Nullable
  public String getInstance() {
    return asMap().get(INSTANCES);
  }

  /**
   * Get the Dataset name.
   *
   * @return the dataset name, or null if not specified.
   */
  @Nullable
  public String getDataset() {
    return asMap().get(DATASETS);
  }

  /**
   * Creates {@link ResourceName} from the provided string in the form of URI path. The components
   * of the asset name will be parsed from the given name.
   *
   * @param name the asset name in the form of URI without leading "/"
   * @return the builder instance
   */
  public static ResourceName fromName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Asset name string should be non-null");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Asset name string should be non-empty");
    }
    return new ResourceName(name, ResourceName.parseNameToComponents(name));
  }

  /**
   * Creates {@link ResourceName} from a list of {@link Component} in the exact order. These
   * components will be used to construct the string representation of the {@link ResourceName}
   *
   * @param components the list of components representing an asset in a hierarchical fashion.
   * @return the builder instance
   */
  public static ResourceName fromComponents(List<Component> components) {
    if (components == null) {
      throw new IllegalArgumentException("Asset name component list should be non-null.");
    }
    if (components.isEmpty()) {
      throw new IllegalArgumentException("Asset name component list should be non-empty.");
    }
    return new ResourceName(createNameFromComponents(components), components);
  }

  private static List<Component> parseNameToComponents(String name) {
    StringTokenizer tokenizer = new StringTokenizer(name, "/");
    if (tokenizer.countTokens() % 2 != 0) {
      throw new IllegalArgumentException(
          "Asset name should have even number of components separated by '/'");
    }
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
}
