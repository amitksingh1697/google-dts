package com.google.cloud.connector.maven.sources;

import com.google.cloud.connector.api.annotation.Config;

/** A configuration class that misses the {@link Config} annotation. */
public class MissingConfig {

  private final String str;

  public MissingConfig(String str) {
    this.str = str;
  }
}
