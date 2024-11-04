package com.google.cloud.connector.server;

import com.google.cloud.bigquery.federation.v1alpha1.DataSource;
import com.google.protobuf.Struct;

/** An interface for describing and instantiating a connector instance. */
public interface ConnectorLoader extends AutoCloseable {

  /** Returns the {@link DataSource} message for describing the connector. */
  DataSource getDataSource();

  /**
   * Creates a new instance of the connector class.
   *
   * @param parameters the set of properties for configuring the connector instance
   * @param parentType the parent class type of the connector to cast to
   * @param <T> the type of the connector parent type
   * @return a new instance of the connector class
   */
  <T> T instantiateConnector(Struct parameters, Class<T> parentType);

  /** Closes and releases all the resources. */
  @Override
  void close();
}
