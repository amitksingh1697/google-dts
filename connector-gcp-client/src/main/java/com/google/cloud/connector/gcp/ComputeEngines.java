package com.google.cloud.connector.gcp;

import static java.net.HttpURLConnection.HTTP_OK;

import com.google.api.pathtemplate.PathTemplate;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Util class for interacting with the Google compute engine. */
public final class ComputeEngines {

  // Doc https://cloud.google.com/compute/docs/metadata/default-metadata-values#project_metadata
  private static final URI METADATA_URI =
      URI.create("http://metadata.google.internal/computeMetadata/");
  private static final URI PROJECT_ID_URI = METADATA_URI.resolve("v1/project/project-id");
  private static final URI PROJECT_NUMBER_URI =
      METADATA_URI.resolve("v1/project/numeric-project-id");

  // Doc https://cloud.google.com/compute/docs/metadata/default-metadata-values#vm_instance_metadata
  private static final URI HOSTNAME_URI = METADATA_URI.resolve("v1/instance/hostname");
  private static final URI INSTANCE_NAME_URI = METADATA_URI.resolve("v1/instance/name");
  private static final URI ZONE_URI = METADATA_URI.resolve("v1/instance/zone");
  private static final URI IP_URL = METADATA_URI.resolve("v1/instance/network-interfaces/0/ip");
  private static final URI IPV6S_URL =
      METADATA_URI.resolve("v1/instance/network-interfaces/0/ipv6s");
  private static final URI NETWORK_URI =
      METADATA_URI.resolve("v1/instance/network-interfaces/0/network");

  private static final PathTemplate ZONE_PATH_TEMPLATE =
      PathTemplate.createWithoutUrlEncoding("projects/{project}/zones/{zone}");

  private static final PathTemplate NETWORK_PATH_TEMPLATE =
      PathTemplate.createWithoutUrlEncoding("projects/{project}/networks/{network}");

  /**
   * Checks if the current process is running inside a Google Compute Engine.
   *
   * @return {@code true} if the current process is running inside a GCP VM.
   */
  public static boolean isComputeEngine() {
    try {
      return Objects.equals(queryMetadata(METADATA_URI.toURL()), "v1/");
    } catch (IOException e) {
      return false;
    }
  }

  /** Returns the project id where the VM belongs to. */
  public static String getProjectId() throws IOException {
    return queryMetadata(PROJECT_ID_URI.toURL());
  }

  /** Returns the project number of the project where the VM belongs to. */
  public static String getProjectNumber() throws IOException {
    return queryMetadata(PROJECT_NUMBER_URI.toURL());
  }

  /** Returns the fully qualified hostname of the VM. */
  public static String getHostname() throws IOException {
    return queryMetadata(HOSTNAME_URI.toURL());
  }

  /** Returns the instance name of the VM. */
  public static String getInstanceName() throws IOException {
    return queryMetadata(INSTANCE_NAME_URI.toURL());
  }

  /** Returns the zone name where the VM is located. */
  public static String getZone() throws IOException {
    String zonePath = queryMetadata(ZONE_URI.toURL());
    Map<String, String> zoneInfo = ZONE_PATH_TEMPLATE.match(zonePath);
    if (zoneInfo == null) {
      throw new IllegalStateException(
          String.format(
              "Invalid zone resource path from the metadata. Expected format is '%s', got '%s'",
              ZONE_PATH_TEMPLATE, zonePath));
    }

    return zoneInfo.get("zone");
  }

  /** Returns the region name based on the zone where the VM is located. */
  public static String getRegion() throws IOException {
    String zone = getZone();
    int idx = zone.lastIndexOf('-');
    if (idx < 0) {
      throw new IOException(
          String.format(
              "Invalid zone format. Expected format is '{region}-{zonePart}', got '%s'", zone));
    }
    return zone.substring(0, idx);
  }

  /** Returns the IPV4 of the current GCE VM. */
  public static Optional<InetAddress> getIpv4() {
    try {
      return Optional.of(InetAddress.getByName(queryMetadata(IP_URL.toURL())));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /** Returns the IPv6 of the current GCE VM. */
  public static Optional<InetAddress> getIpv6() {
    try {
      return Optional.of(InetAddress.getByName(queryMetadata(IPV6S_URL.toURL())));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /**
   * Returns the VPC network of the current GCE VM from VM metadata in the form of
   * projects/{project}/global/networks/{network}.
   */
  public static String getNetwork() throws IOException {
    String networkPath = queryMetadata(NETWORK_URI.toURL());
    Map<String, String> networkInfo = NETWORK_PATH_TEMPLATE.match(networkPath);
    if (networkInfo == null) {
      throw new IllegalStateException(
          String.format(
              "Invalid network resource path from the metadata. Expected format is '%s', got '%s'",
              NETWORK_PATH_TEMPLATE, networkPath));
    }
    return String.format(
        "projects/%s/global/networks/%s", getProjectId(), networkInfo.get("network"));
  }

  /**
   * Query GCE VM metadata via the provided URL to fetch metadata values for the current VM. Note
   * the returned string is trimmed.
   */
  private static String queryMetadata(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    try {
      connection.setRequestProperty("Metadata-Flavor", "Google");
      connection.connect();
      int rc = connection.getResponseCode();
      if (rc != HTTP_OK) {
        throw new IOException("Fail to query metadata server. Response code = " + rc);
      }
      if (!Objects.equals(connection.getHeaderField("Metadata-Flavor"), "Google")) {
        throw new IOException("Expected to have response header 'Metadata-Flavor: Google'");
      }

      try (Reader reader =
          new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
        return CharStreams.toString(reader).trim();
      }
    } finally {
      connection.disconnect();
    }
  }

  private ComputeEngines() {}
}
