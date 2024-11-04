package com.google.cloud.connector.gcp;

import com.google.cloud.connector.api.discovery.EndpointRegistry;
import com.google.cloud.servicedirectory.v1.EndpointName;
import com.google.cloud.servicedirectory.v1.ServiceName;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.Nullable;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;

/**
 * This class carries the configuration for the {@link ServiceDirectoryEndpointRegistry}.
 */
public final class ServiceDirectoryEndpointConfig {

  @Option(names = {"-s6",
      "--service-directory-ipv6"}, converter = ServiceNameConverter.class, description = """
      If present and IPV6 address is enabled, the resource path of the GCP Service Directory
      Service for this server to register IPV6 address under.
      Accepted formats are:

      namespaces/{namespace}/services/{service}
      locations/{location}/namespaces/{namespace}/services/{service}
      projects/{project}/locations/{location}/namespaces/{namespace}/services/{service}

      The project id or location can be missing when executing in GCP VM, which they will be
      determined by the VM metadata.
      """)
  @Nullable
  private ServiceName ipv6ServiceName;

  @Option(names = {"-s4",
      "--service-directory-ipv4"}, converter = ServiceNameConverter.class, description = """
      If present and IPV4 address is enabled, the resource path of the GCP Service Directory
      Service for this server to register IPV4 address under.
      Accepted formats are:

      namespaces/{namespace}/services/{service}
      locations/{location}/namespaces/{namespace}/services/{service}
      projects/{project}/locations/{location}/namespaces/{namespace}/services/{service}

      The project id or location can be missing when executing in GCP VM, which they will be
      determined by the VM metadata.
      """)
  @Nullable
  private ServiceName ipv4ServiceName;

  @Option(names = {"-c",
      "--service-directory-create-parent"}, description = "Automatically create missing parent resources for the endpoint.")
  private boolean createParent;

  @SuppressWarnings("unused")
  public ServiceDirectoryEndpointConfig() {
    // no-op for picocli to use
  }

  @VisibleForTesting
  ServiceDirectoryEndpointConfig(
      @Nullable ServiceName ipv6ServiceName,
      @Nullable ServiceName ipv4ServiceName,
      boolean createParent) {
    this.ipv6ServiceName = ipv6ServiceName;
    this.ipv4ServiceName = ipv4ServiceName;
    this.createParent = createParent;
  }

  /**
   * Returns {@code true} if it is configured for {@link ServiceDirectoryEndpointRegistry}.
   */
  public boolean isConfigured() {
    return ipv6ServiceName != null || ipv4ServiceName != null;
  }

  public Optional<ServiceName> getIpv6ServiceName() {
    return Optional.ofNullable(ipv6ServiceName);
  }

  public Optional<ServiceName> getIpv4ServiceName() {
    return Optional.ofNullable(ipv4ServiceName);
  }

  public boolean isCreateParent() {
    return createParent;
  }

  private static final class ServiceNameConverter implements ITypeConverter<ServiceName> {

    /**
     * Creates a {@link EndpointName} based on the string path.
     *
     * @param path the resource path to the service directory. Accepted formats are:
     *             <ul>
     *               <li>namespaces/{namespace}/services/{service}
     *               <li>locations/{location}/namespaces/{namespace}/services/{service}
     *               <li>projects/{project}/locations/{location}/namespaces/{namespace}/services/{service}
     *             </ul>
     *             If the project or location is missing, it will be determined from the metadata of the VM.
     */
    @Override
    public ServiceName convert(String path) {
      try {
        if (ComputeEngines.isComputeEngine()) {
          if (path.startsWith("locations")) {
            return ServiceName.parse(
                String.format("projects/%s/%s", ComputeEngines.getProjectId(), path));
          }
          if (path.startsWith("namespaces")) {
            return ServiceName.parse(
                String.format("projects/%s/locations/%s/%s", ComputeEngines.getProjectId(),
                    ComputeEngines.getRegion(), path));
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException(
            String.format("Failed to derive the full service directory resource name from '%s'",
                path), e);
      }

      return ServiceName.parse(path);
    }
  }
}
