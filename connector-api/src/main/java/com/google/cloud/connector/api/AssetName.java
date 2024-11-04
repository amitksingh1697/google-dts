package com.google.cloud.connector.api;

import com.google.cloud.bigquery.federation.v1alpha1.NamedTable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the fully qualified resource name of an asset, which is in the form of a URI path
 * without leading "/" (e.g. "my-database/my-schema/my-table").
 *
 * @param name     Resource name for the AssetName, in the form of element1/element2/...
 * @param elements list of elements in the resource name hierarchy
 */
public record AssetName(String name, List<String> elements) {
  // Represents the root level, at which, the asset name and elements will be empty.
  public static final AssetName ROOT_ASSET = new AssetName("", Collections.emptyList());

  public AssetName(String name, List<String> elements) {
    this.name = name;
    this.elements = List.copyOf(elements);
  }

  public AssetName(List<String> elements) {
    this(createNameFromElements(elements), elements);
  }

  /**
   * Creates a new {@link AssetName} that prepends the given element.
   *
   * @param element element to prepend
   * @return a new {@link AssetName}
   */
  public AssetName prepend(String element) {
    List<String> newElements =
        ImmutableList.<String>builder().add(element).addAll(elements).build();
    return new AssetName(newElements);
  }

  /**
   * Creates a new {@link AssetName} that appends the given element.
   *
   * @param element element to append
   * @return a new {@link AssetName}
   */
  public AssetName append(String element) {
    List<String> newElements =
        ImmutableList.<String>builder().addAll(elements).add(element).build();
    return new AssetName(newElements);
  }

  /**
   * Build an instance of a {@link AssetName} from a supplied {@link NamedTable}.
   *
   * @param namedTable the named table used to build this {@link AssetName}
   * @return a {@link AssetName} instance.
   */
  public static AssetName fromNamedTable(NamedTable namedTable) {
    Preconditions.checkArgument(namedTable != null, "The namedTable should be non-null.");
    return new AssetName(namedTable.getNamesList());
  }

  /**
   * Get the number of elements in this hierarchical asset name.
   */
  public int getNumElements() {
    return elements.size();
  }

  /**
   * Get a name element from the asset name based on the element position.
   *
   * @return name element in the specified position
   * @throws IllegalArgumentException when the position is out-of-bounds.
   */
  public String getElement(int pos) {
    if (pos < 0 || pos >= elements.size()) {
      throw new IllegalArgumentException(
          String.format(
              "Unable to get element in position %d. Number of elements: %d",
              pos, elements.size()));
    }
    return elements.get(pos);
  }

  @Override
  public String toString() {
    return name;
  }

  private static String createNameFromElements(List<String> elements) {
    return String.join("/", elements);
  }
}
