package com.google.cloud.connector.gcp;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.servicedirectory.v1.Endpoint;
import com.google.cloud.servicedirectory.v1.EndpointName;
import com.google.cloud.servicedirectory.v1.LocationName;
import com.google.cloud.servicedirectory.v1.Namespace;
import com.google.cloud.servicedirectory.v1.NamespaceName;
import com.google.cloud.servicedirectory.v1.RegistrationServiceClient;
import com.google.cloud.servicedirectory.v1.Service;
import com.google.cloud.servicedirectory.v1.ServiceName;
import io.grpc.Status.Code;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit tests for the {@link ServiceDirectoryEndpointRegistry} class. */
public class ServiceDirectoryEndpointRegistryTest {

  @RegisterExtension
  private static final MockMetadataServer metadataServer = new MockMetadataServer();

  private static final String ENDPOINT_NAME =
      "test-instance-name-with-special-chars-and-longer-than-sixty-thr";

  @Test
  public void register_throwsIfServiceNameNotProvided() throws IOException {
    RegistrationServiceClient client = mock(RegistrationServiceClient.class);
    ServiceDirectoryEndpointRegistry registry =
        new ServiceDirectoryEndpointRegistry(new ServiceDirectoryEndpointConfig(null, null, false));

    registry = spy(registry);
    doReturn(client).when(registry).getClient();

    ServiceDirectoryEndpointRegistry finalRegistry = registry;
    assertThrows(IllegalArgumentException.class, () -> finalRegistry.register(54321).close());
  }

  @Test
  public void register_createOnlyIpv4EndpointIfIpv6ServiceNameNotPresent() throws IOException {
    RegistrationServiceClient client = mock(RegistrationServiceClient.class);
    when(client.createEndpoint(any(ServiceName.class), any(), anyString()))
        .thenReturn(Endpoint.newBuilder().build());

    NamespaceName namespaceName = NamespaceName.of("project", "us-west1", "ns");
    ServiceName serviceNameIpv4 = ServiceName.parse(namespaceName + "/services/s1ipv4");
    ServiceDirectoryEndpointRegistry registry =
        new ServiceDirectoryEndpointRegistry(
            new ServiceDirectoryEndpointConfig(null, serviceNameIpv4, false));
    registry = spy(registry);
    doReturn(client).when(registry).getClient();

    registry.register(54321).close();

    verify(client, times(1)).createEndpoint(any(ServiceName.class), any(), anyString());
    verify(client)
        .createEndpoint(eq(serviceNameIpv4), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv4 + "/endpoints/" + ENDPOINT_NAME)));
    verify(client, times(1)).deleteEndpoint(any(EndpointName.class));
  }

  @Test
  public void register_createOnlyIpv6EndpointIfIpv4ServiceNameNotPresent() throws IOException {
    RegistrationServiceClient client = mock(RegistrationServiceClient.class);
    when(client.createEndpoint(any(ServiceName.class), any(), anyString()))
        .thenReturn(Endpoint.newBuilder().build());

    NamespaceName namespaceName = NamespaceName.of("project", "us-west1", "ns");
    ServiceName serviceNameIpv6 = ServiceName.parse(namespaceName + "/services/s1ipv6");
    ServiceDirectoryEndpointRegistry registry =
        new ServiceDirectoryEndpointRegistry(
            new ServiceDirectoryEndpointConfig(serviceNameIpv6, null, false));
    registry = spy(registry);
    doReturn(client).when(registry).getClient();

    registry.register(54321).close();

    verify(client, times(1)).createEndpoint(any(ServiceName.class), any(), anyString());
    verify(client)
        .createEndpoint(eq(serviceNameIpv6), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv6 + "/endpoints/" + ENDPOINT_NAME)));
    verify(client, times(1)).deleteEndpoint(any(EndpointName.class));
  }

  @Test
  public void register_createEndpointWithParentExists() throws IOException {
    RegistrationServiceClient client = mock(RegistrationServiceClient.class);
    when(client.createEndpoint(any(ServiceName.class), any(), anyString()))
        .thenReturn(Endpoint.newBuilder().build());

    NamespaceName namespaceName = NamespaceName.of("project", "us-west1", "ns");
    ServiceName serviceNameIpv6 = ServiceName.parse(namespaceName + "/services/s1ipv6");
    ServiceName serviceNameIpv4 = ServiceName.parse(namespaceName + "/services/s1ipv4");
    ServiceDirectoryEndpointRegistry registry =
        new ServiceDirectoryEndpointRegistry(
            new ServiceDirectoryEndpointConfig(serviceNameIpv6, serviceNameIpv4, false));
    registry = spy(registry);
    doReturn(client).when(registry).getClient();

    registry.register(54321).close();

    verify(client)
        .createEndpoint(eq(serviceNameIpv6), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client)
        .createEndpoint(eq(serviceNameIpv4), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv6 + "/endpoints/" + ENDPOINT_NAME)));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv4 + "/endpoints/" + ENDPOINT_NAME)));
  }

  @Test
  public void register_updateEndpoint() throws IOException {
    RegistrationServiceClient client = mock(RegistrationServiceClient.class);
    when(client.createEndpoint(any(ServiceName.class), any(), anyString()))
        .thenThrow(new ApiException(null, GrpcStatusCode.of(Code.ALREADY_EXISTS), false));
    when(client.updateEndpoint(any(), any())).thenAnswer(a -> a.getArgument(0));

    NamespaceName namespaceName = NamespaceName.of("project", "us-west1", "ns");
    ServiceName serviceNameIpv6 = ServiceName.parse(namespaceName + "/services/s1ipv6");
    ServiceName serviceNameIpv4 = ServiceName.parse(namespaceName + "/services/s1ipv4");
    ServiceDirectoryEndpointRegistry registry =
        new ServiceDirectoryEndpointRegistry(
            new ServiceDirectoryEndpointConfig(serviceNameIpv6, serviceNameIpv4, false));
    registry = spy(registry);
    doReturn(client).when(registry).getClient();

    registry.register(54321).close();

    verify(client)
        .createEndpoint(eq(serviceNameIpv6), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client)
        .createEndpoint(eq(serviceNameIpv4), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client, times(2))
        .updateEndpoint(
            argThat(e -> e.getPort() == 54321),
            argThat(ServiceDirectoryEndpointRegistry.ENDPOINT_UPDATE_FIELD_MASK::equals));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv6 + "/endpoints/" + ENDPOINT_NAME)));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv4 + "/endpoints/" + ENDPOINT_NAME)));
  }

  @Test
  public void register_createEndpointAndParent() throws IOException {
    RegistrationServiceClient client = mock(RegistrationServiceClient.class);
    when(client.createEndpoint(any(ServiceName.class), any(), anyString()))
        .thenThrow(new ApiException(null, GrpcStatusCode.of(Code.NOT_FOUND), false))
        .thenReturn(Endpoint.newBuilder().build())
        .thenThrow(new ApiException(null, GrpcStatusCode.of(Code.NOT_FOUND), false))
        .thenReturn(Endpoint.newBuilder().build());
    when(client.createService(any(NamespaceName.class), any(), anyString()))
        .thenThrow(new ApiException(null, GrpcStatusCode.of(Code.NOT_FOUND), false))
        .thenReturn(Service.newBuilder().build())
        .thenThrow(new ApiException(null, GrpcStatusCode.of(Code.NOT_FOUND), false))
        .thenReturn(Service.newBuilder().build());
    when(client.createNamespace(any(LocationName.class), any(), anyString()))
        .thenReturn(Namespace.newBuilder().build())
        .thenThrow(new ApiException(null, GrpcStatusCode.of(Code.ALREADY_EXISTS), false));

    NamespaceName namespaceName = NamespaceName.of("project", "us-west1", "ns");
    ServiceName serviceNameIpv6 = ServiceName.parse(namespaceName + "/services/s1ipv6");
    ServiceName serviceNameIpv4 = ServiceName.parse(namespaceName + "/services/s1ipv4");
    ServiceDirectoryEndpointRegistry registry =
        new ServiceDirectoryEndpointRegistry(
            new ServiceDirectoryEndpointConfig(serviceNameIpv6, serviceNameIpv4, true));
    registry = spy(registry);
    doReturn(client).when(registry).getClient();

    registry.register(54321).close();

    verify(client, times(2))
        .createEndpoint(eq(serviceNameIpv6), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client, times(2))
        .createEndpoint(eq(serviceNameIpv4), argThat(e -> e.getPort() == 54321), eq(ENDPOINT_NAME));
    verify(client, times(2))
        .createService(
            eq(namespaceName),
            argThat(s -> s.getName().equals(serviceNameIpv6.toString())),
            eq(serviceNameIpv6.getService()));
    verify(client, times(2))
        .createService(
            eq(namespaceName),
            argThat(s -> s.getName().equals(serviceNameIpv4.toString())),
            eq(serviceNameIpv4.getService()));
    verify(client, times(2))
        .createNamespace(
            eq(LocationName.of(namespaceName.getProject(), namespaceName.getLocation())),
            argThat(n -> n.getName().equals(namespaceName.toString())),
            eq(namespaceName.getNamespace()));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv6 + "/endpoints/" + ENDPOINT_NAME)));
    verify(client)
        .deleteEndpoint(eq(EndpointName.parse(serviceNameIpv4 + "/endpoints/" + ENDPOINT_NAME)));
  }
}
