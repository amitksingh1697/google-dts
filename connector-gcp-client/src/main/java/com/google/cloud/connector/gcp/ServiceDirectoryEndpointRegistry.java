package com.google.cloud.connector.gcp;

import static com.google.api.gax.rpc.StatusCode.Code.ALREADY_EXISTS;
import static com.google.api.gax.rpc.StatusCode.Code.NOT_FOUND;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.connector.api.discovery.EndpointRegistry;
import com.google.cloud.servicedirectory.v1.Endpoint;
import com.google.cloud.servicedirectory.v1.EndpointName;
import com.google.cloud.servicedirectory.v1.LocationName;
import com.google.cloud.servicedirectory.v1.Namespace;
import com.google.cloud.servicedirectory.v1.NamespaceName;
import com.google.cloud.servicedirectory.v1.RegistrationServiceClient;
import com.google.cloud.servicedirectory.v1.Service;
import com.google.cloud.servicedirectory.v1.ServiceName;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.flogger.FluentLogger;
import com.google.protobuf.FieldMask;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** A {@link EndpointRegistry} that register endpoint to the GCP service directory. */
public class ServiceDirectoryEndpointRegistry implements EndpointRegistry {

  @VisibleForTesting
  static final FieldMask ENDPOINT_UPDATE_FIELD_MASK =
      FieldMask.newBuilder().addPaths("address").addPaths("port").build();

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  // The maximum length of a service directory endpoint name.
  private static final int ENDPOINT_MAX_LENGTH = 63;
  private final ServiceDirectoryEndpointConfig endpointConfig;

  public ServiceDirectoryEndpointRegistry(ServiceDirectoryEndpointConfig endpointConfig) {
    this.endpointConfig = endpointConfig;
  }

  @Override
  public Closeable register(int port) throws IOException {
    if (!endpointConfig.isConfigured()) {
      throw new IllegalArgumentException("ServiceDirectoryEndpointConfig is not configured.");
    }

    try (RegistrationServiceClient client = getClient()) {
      Optional<InetAddress> ipv6;
      Optional<InetAddress> ipv4;

      if (ComputeEngines.isComputeEngine()) {
        ipv6 = ComputeEngines.getIpv6();
        ipv4 = ComputeEngines.getIpv4();
      } else {
        ipv6 = getAddressByType(Inet6Address.class);
        ipv4 = getAddressByType(Inet4Address.class);
      }

      endpointConfig.getIpv6ServiceName()
          .ifPresent(serviceName ->
              ipv6.ifPresent(addr ->
                  registerEndpoint(client, serviceName, addr, port)));

      endpointConfig.getIpv4ServiceName()
          .ifPresent(serviceName ->
              ipv4.ifPresent(addr ->
                  registerEndpoint(client, serviceName, addr, port)));
    }

    return this::deleteEndpointsIfExists;
  }

  private Optional<InetAddress> getAddressByType(Class<?> type) throws UnknownHostException {
    InetAddress localHost = InetAddress.getLocalHost();
    return Arrays.stream(InetAddress.getAllByName(localHost.getHostName()))
        .filter(type::isInstance)
        .filter(this::isExternalAccessible)
        .findFirst();
  }

  private boolean isExternalAccessible(InetAddress addr) {
    return !addr.isLoopbackAddress()
        && !addr.isLinkLocalAddress()
        && !addr.isSiteLocalAddress()
        && !addr.isMulticastAddress();
  }

  private void registerEndpoint(
      RegistrationServiceClient client, ServiceName serviceName, InetAddress address, int port) {
    EndpointName endpointName = getEndpointNameFromService(serviceName);
    Endpoint endpoint =
        Endpoint.newBuilder()
            .setName(endpointName.toString())
            .setAddress(address.getHostAddress())
            .setPort(port)
            .build();

    logger.atInfo().log(
        "Registering server '%s:%d' with service directory endpoint '%s'",
        address.getHostAddress(), port, endpointName);

    createOrUpdateEndpoint(client, serviceName, endpoint, endpointName.getEndpoint());
  }

  private static String getNormalizedEndpointName() {
    String endpointFullName;
    try {
      if (ComputeEngines.isComputeEngine()) {
        // Use INSTANCE_NAME as endpoint name if it's GCE VM
        endpointFullName = ComputeEngines.getInstanceName();
      } else {
        // Use a-{HOSTNAME} name otherwise
        endpointFullName = String.format("a-%s", InetAddress.getLocalHost().getHostName());
        logger.atWarning().log(
            "Registering service directory endpoint with hostname '%s'", endpointFullName);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // Endpoint name has to start with a lowercase letter followed by up to 62 lowercase letters,
    // numbers, or hyphens, and cannot end with a hyphen.
    String normalizedEndpointName =
        Ascii.toLowerCase(endpointFullName.replaceAll("\\W+", "-").replaceAll("-+$", ""));
    return normalizedEndpointName.substring(
        0, Math.min(ENDPOINT_MAX_LENGTH, normalizedEndpointName.length()));
  }

  @VisibleForTesting
  RegistrationServiceClient getClient() throws IOException {
    return RegistrationServiceClient.create();
  }

  private void createOrUpdateEndpoint(
      RegistrationServiceClient client,
      ServiceName serviceName,
      Endpoint endpoint,
      String endpointId) {

    try {
      client.createEndpoint(serviceName, endpoint, endpointId);
    } catch (ApiException e) {
      switch (e.getStatusCode().getCode()) {
        case ALREADY_EXISTS -> client.updateEndpoint(endpoint, ENDPOINT_UPDATE_FIELD_MASK);
        case NOT_FOUND -> {
          if (endpointConfig.isCreateParent()) {
            createServiceIfNotExists(client, serviceName);
            createOrUpdateEndpoint(client, serviceName, endpoint, endpointId);
          } else {
            throw e;
          }
        }
        default -> throw e;
      }
    }
  }

  private void createServiceIfNotExists(RegistrationServiceClient client, ServiceName serviceName) {
    NamespaceName namespaceName =
        NamespaceName.of(
            serviceName.getProject(), serviceName.getLocation(), serviceName.getNamespace());
    try {
      client.createService(
          namespaceName,
          Service.newBuilder().setName(serviceName.toString()).build(),
          serviceName.getService());
    } catch (ApiException e) {
      switch (e.getStatusCode().getCode()) {
        case ALREADY_EXISTS -> {
          // no-op, just return
        }
        case NOT_FOUND -> {
          // Create the parent and try the service creation again
          createNamespaceIfNotExists(client, namespaceName);
          createServiceIfNotExists(client, serviceName);
        }
        default -> throw e;
      }
    }
  }

  private void createNamespaceIfNotExists(
      RegistrationServiceClient client, NamespaceName namespaceName) {

    try {
      client.createNamespace(
          LocationName.of(namespaceName.getProject(), namespaceName.getLocation()),
          Namespace.newBuilder().setName(namespaceName.toString()).build(),
          namespaceName.getNamespace());
    } catch (ApiException e) {
      if (e.getStatusCode().getCode() != ALREADY_EXISTS) {
        throw e;
      }
    }
  }

  private void deleteEndpointsIfExists() throws IOException {
    try (RegistrationServiceClient client = getClient()) {
      for (Optional<ServiceName> serviceName :
          List.of(endpointConfig.getIpv6ServiceName(), endpointConfig.getIpv4ServiceName())) {
        serviceName
            .map(this::getEndpointNameFromService)
            .ifPresent(endpoint -> deleteEndpointIfExists(client, endpoint));
      }
    }
  }

  private void deleteEndpointIfExists(RegistrationServiceClient client, EndpointName endpointName) {
    try {
      client.deleteEndpoint(endpointName);
      logger.atInfo().log("Deleted service directory endpoint '%s'", endpointName);
    } catch (ApiException e) {
      if (e.getStatusCode().getCode().equals(NOT_FOUND)) {
        logger.atInfo().log(
            "Service directory endpoint '%s' does not exist thus no need to be deleted.",
            endpointName);
        return;
      }

      throw new RuntimeException(
          String.format("Failed to delete service directory endpoint '%s'", endpointName));
    }
  }

  private EndpointName getEndpointNameFromService(ServiceName serviceName) {
    return EndpointName.of(
        serviceName.getProject(),
        serviceName.getLocation(),
        serviceName.getNamespace(),
        serviceName.getService(),
        getNormalizedEndpointName());
  }
}
