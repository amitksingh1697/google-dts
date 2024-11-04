package com.google.cloud.connector.server;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.cloud.connector.api.Connector;
import com.google.cloud.connector.maven.ConnectorGeneratorMojo;
import com.google.cloud.connector.server.connector.NoParamConnector;
import com.google.cloud.connector.server.connector.TestConnector;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/** Unit test for {@link LocalPathConnectorLoader}. */
public class LocalPathConnectorLoaderTest {

  @Test
  public void instantiate_ConfigConnector() throws Exception {
    Path basePath = getClassBasePath(TestConnector.class);
    ConnectorGeneratorMojo.create(
            basePath, basePath, TestConnector.class.getName(), "connector.proto")
        .execute();

    try (ConnectorLoader loader = new LocalPathConnectorLoader(basePath, "connector.proto")) {
      Connector instance =
          loader.instantiateConnector(
              Struct.newBuilder()
                  .putFields("endpoint.host", Value.newBuilder().setStringValue("testhost").build())
                  .putFields("endpoint.port", Value.newBuilder().setNumberValue(43210).build())
                  .putFields(
                      "authentication.oauth.clientId",
                      Value.newBuilder().setStringValue("oauthclientid").build())
                  .putFields("threshold", Value.newBuilder().setNumberValue(0.6d).build())
                  .putFields("timeout", Value.newBuilder().setNumberValue(10000L).build())
                  .build(),
              Connector.class);

      assertThat(instance.getClass().getClassLoader())
          .isNotSameInstanceAs(getClass().getClassLoader());
      assertThat(instance.toString())
          .isEqualTo(
              String.format(
                  "%s,%s,%d,%s,%.2f,%d",
                  TestConnector.class.getName(), "testhost", 43210, "oauthclientid", 0.6d, 10000L));
    }
  }

  @Test
  public void instantiate_NoParamConnector() throws Exception {
    Path basePath = getClassBasePath(NoParamConnector.class);
    ConnectorGeneratorMojo.create(
            basePath, basePath, NoParamConnector.class.getName(), "noparam.proto")
        .execute();

    try (ConnectorLoader loader = new LocalPathConnectorLoader(basePath, "noparam.proto")) {
      Connector instance =
          loader.instantiateConnector(Struct.getDefaultInstance(), Connector.class);

      assertThat(instance.getClass().getClassLoader())
          .isNotSameInstanceAs(getClass().getClassLoader());
      assertThat(instance.toString()).isEqualTo(NoParamConnector.class.getName());
    }
  }

  @Test
  public void fail_missingConfig() throws Exception {
    Path basePath = getClassBasePath(TestConnector.class);
    ConnectorGeneratorMojo.create(
            basePath, basePath, TestConnector.class.getName(), "missing_config.proto")
        .execute();
    try (ConnectorLoader loader = new LocalPathConnectorLoader(basePath, "missing_config.proto")) {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> loader.instantiateConnector(Struct.newBuilder().build(), Connector.class));
      assertThat(exception.getMessage()).startsWith("Null value is not allowed");
    }
  }

  private Path getClassBasePath(Class<?> cls) throws URISyntaxException {
    String classFile = cls.getName().replace('.', File.separatorChar) + ".class";
    URL resource = getClass().getClassLoader().getResource(classFile);
    assertNotNull(resource);

    String uriStr = resource.toURI().toString();
    return Paths.get(URI.create(uriStr.substring(0, uriStr.length() - classFile.length())));
  }
}
