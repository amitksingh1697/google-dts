package com.google.cloud.connector.maven.sources;

import com.google.cloud.connector.api.config.HostAndPort;
import java.net.URL;

/** A config class for encapsulating endpoint information. */
public record EndpointConfig(URL url, HostAndPort hostAndPort, String genericEndpoint) {}
