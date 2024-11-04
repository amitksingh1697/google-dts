package com.google.cloud.connector.api;

import static com.google.cloud.connector.api.AssetName.ROOT_ASSET;
import static com.google.cloud.connector.api.AssetName.fromNamedTable;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.cloud.bigquery.federation.v1alpha1.NamedTable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AssetName}.
 */
public class AssetNameTest {
  private static final String DATABASE = "mydb";
  private static final String SCHEMA = "myschema";
  private static final String TABLE = "mytable";
  private static final List<String> ELEMENT_LIST = List.of(DATABASE, SCHEMA, TABLE);

  private static final String ASSET_NAME = String.format("%s/%s/%s", DATABASE, SCHEMA, TABLE);
  private static final NamedTable NAMED_TABLE =
      NamedTable.newBuilder().addAllNames(ELEMENT_LIST).build();

  @Test
  public void fromNamedTable_returnsValidAsset() {
    AssetName assetName = AssetName.fromNamedTable(NAMED_TABLE);

    assertThat(assetName.name()).isEqualTo(ASSET_NAME);
    assertThat(assetName.getNumElements()).isEqualTo(3);
    assertThat(assetName.elements()).containsExactlyElementsIn(ELEMENT_LIST).inOrder();
    assertThat(assetName.getElement(0)).isEqualTo(DATABASE);
  }

  @Test
  public void fromEmptyNamedTable_returnsRootAsset() {
    AssetName assetName = AssetName.fromNamedTable(NamedTable.getDefaultInstance());
    assertThat(assetName).isEqualTo(ROOT_ASSET);
  }

  @Test
  public void fromNullNamedTable_fails() {
    assertThrows(IllegalArgumentException.class, () -> fromNamedTable(null));
  }

  @Test
  public void getInvalidElement_fails() {
    AssetName assetName = AssetName.fromNamedTable(NamedTable.getDefaultInstance());
    assertThrows(IllegalArgumentException.class, () -> assetName.getElement(0));
  }

  @Test
  public void prependElement_returnsValidAsset() {
    String newElement = "random";
    AssetName assetName = AssetName.fromNamedTable(NAMED_TABLE);
    List<String> expectedElements =
        ImmutableList.<String>builder().add(newElement).addAll(ELEMENT_LIST).build();

    AssetName prependedAssetName = assetName.prepend(newElement);

    assertThat(prependedAssetName.elements()).containsExactlyElementsIn(expectedElements).inOrder();
  }

  @Test
  public void appendElement_returnsValidAsset() {
    String newElement = "random";
    AssetName assetName = AssetName.fromNamedTable(NAMED_TABLE);
    List<String> expectedElements =
        ImmutableList.<String>builder().addAll(ELEMENT_LIST).add(newElement).build();

    AssetName appendedAssetName = assetName.append(newElement);

    assertThat(appendedAssetName.elements()).containsExactlyElementsIn(expectedElements).inOrder();
  }
}
