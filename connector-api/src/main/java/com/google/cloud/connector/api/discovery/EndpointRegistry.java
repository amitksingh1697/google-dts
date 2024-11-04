package com.google.cloud.connector.api.discovery;

import java.io.Closeable;
import java.io.IOException;

/** An interface to provide service registration for local services. */
public interface EndpointRegistry {

  /** An no-op {@link EndpointRegistry}. */
  EndpointRegistry NOOP = addr -> () -> {};

  /** Register a service that binds to the given port. */
  Closeable register(int port) throws IOException;
}
