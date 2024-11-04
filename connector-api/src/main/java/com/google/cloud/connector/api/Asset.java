package com.google.cloud.connector.api;

import com.google.cloud.bigquery.federation.v1alpha1.Schema;

/**
 * Represents a collection of records stored in an external system (for example, a
 * Database/BigQuery table). An Asset consists of an Asset name, along with the schema for the
 * records contained within this asset.
 */
public record Asset(AssetName name, Schema schema) {
}
