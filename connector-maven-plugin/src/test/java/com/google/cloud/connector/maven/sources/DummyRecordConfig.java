package com.google.cloud.connector.maven.sources;

import com.google.cloud.connector.api.annotation.Parameter;

/** A record config class for testing. */
public record DummyRecordConfig(@Parameter("host") String hostname, int port) {}
