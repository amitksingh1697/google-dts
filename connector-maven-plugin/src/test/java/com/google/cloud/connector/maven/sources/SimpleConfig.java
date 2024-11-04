package com.google.cloud.connector.maven.sources;

import com.google.cloud.connector.api.annotation.Config;
import com.google.cloud.connector.api.annotation.Parameter;

/** A regular config class for testing. */
public class SimpleConfig {

  private final String uri;
  private final Long timeout;

  @Config
  public SimpleConfig(@Parameter("uri") String uri, @Parameter("timeout") Long timeout) {
    this.uri = uri;
    this.timeout = timeout;
  }
}
