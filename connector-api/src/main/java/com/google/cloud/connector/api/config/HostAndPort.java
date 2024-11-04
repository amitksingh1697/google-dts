package com.google.cloud.connector.api.config;

import static com.google.cloud.connector.api.annotation.Semantic.Category.ENDPOINT;

import com.google.cloud.connector.api.annotation.Semantic;

/** A record for representing host and port configuration. */
@Semantic(ENDPOINT)
public record HostAndPort(String host, int port) {
  @Override
  public String toString() {
    return String.format("%s:%s", host, port);
  }
}