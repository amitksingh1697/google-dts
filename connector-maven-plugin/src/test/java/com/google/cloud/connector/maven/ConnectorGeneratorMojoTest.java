package com.google.cloud.connector.maven;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.cloud.connector.Connector;
import com.google.cloud.connector.api.annotation.Config;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.maven.sources.AllCapabilitiesDataSource;
import com.google.cloud.connector.maven.sources.DirectConfigDataSource;
import com.google.cloud.connector.maven.sources.DummyRecordConfigDataSource;
import com.google.cloud.connector.maven.sources.MissingConfig;
import com.google.cloud.connector.maven.sources.MissingConfigDataSource;
import com.google.cloud.connector.maven.sources.MissingDataSource;
import com.google.cloud.connector.maven.sources.MultipleDataSource;
import com.google.cloud.connector.maven.sources.NoParamDataSource;
import com.google.cloud.connector.maven.sources.ParamMissingValueDataSource;
import com.google.cloud.connector.maven.sources.SemanticDataSource;
import com.google.cloud.connector.maven.sources.SimpleDataSource;
import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit test for the {@link ConnectorGeneratorMojo}. */
public class ConnectorGeneratorMojoTest {

  @Test
  public void generate_recordConfig(@TempDir Path targetDir) throws Exception {
    String protoFileName = "dummy.textproto";
    createMojo(targetDir, DummyRecordConfigDataSource.class, protoFileName).execute();

    Connector expected =
        TextFormat.parse(
            Resources.toString(Resources.getResource("dummy.datasource.textproto"), UTF_8),
            Connector.class);
    Connector generated =
        TextFormat.parse(
            Files.readString(targetDir.resolve(protoFileName), UTF_8), Connector.class);

    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void generate_classConfig(@TempDir Path targetDir) throws Exception {
    String protoFileName = "simple.textproto";
    createMojo(targetDir, SimpleDataSource.class, protoFileName).execute();

    Connector expected =
        TextFormat.parse(
            Resources.toString(Resources.getResource("simple.datasource.textproto"), UTF_8),
            Connector.class);
    Connector generated =
        TextFormat.parse(
            Files.readString(targetDir.resolve(protoFileName), UTF_8), Connector.class);

    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void generate_noConfigClass(@TempDir Path targetDir) throws Exception {
    String protoFileName = "direct.textproto";
    createMojo(targetDir, DirectConfigDataSource.class, protoFileName).execute();

    Connector expected =
        TextFormat.parse(
            Resources.toString(Resources.getResource("direct.datasource.textproto"), UTF_8),
            Connector.class);
    Connector generated =
        TextFormat.parse(
            Files.readString(targetDir.resolve(protoFileName), UTF_8), Connector.class);

    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void generate_noParameters(@TempDir Path targetDir) throws Exception {
    String protoFileName = "noparam.textproto";
    createMojo(targetDir, NoParamDataSource.class, protoFileName).execute();

    Connector expected =
        TextFormat.parse(
            Resources.toString(Resources.getResource("noparam.datasource.textproto"), UTF_8),
            Connector.class);
    Connector generated =
        TextFormat.parse(
            Files.readString(targetDir.resolve(protoFileName), UTF_8), Connector.class);

    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void generate_allCapabilities(@TempDir Path targetDir) throws Exception {
    String protoFileName = "allcapabilities.textproto";
    createMojo(targetDir, AllCapabilitiesDataSource.class, protoFileName).execute();

    Connector expected =
        TextFormat.parse(
            Resources.toString(
                Resources.getResource("allcapabilities.datasource.textproto"), UTF_8),
            Connector.class);
    Connector generated =
        TextFormat.parse(
            Files.readString(targetDir.resolve(protoFileName), UTF_8), Connector.class);

    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void generate_semanticTypes(@TempDir Path targetDir) throws Exception {
    String protoFileName = "semanticType.textproto";
    createMojo(targetDir, SemanticDataSource.class, protoFileName).execute();

    Connector expected =
        TextFormat.parse(
            Resources.toString(Resources.getResource("semantic.datasource.textproto"), UTF_8),
            Connector.class);
    Connector generated =
        TextFormat.parse(
            Files.readString(targetDir.resolve(protoFileName), UTF_8), Connector.class);

    assertThat(generated).isEqualTo(expected);
  }

  @Test
  public void fail_missingDataSource(@TempDir Path targetDir) {
    MojoExecutionException exception =
        assertThrows(
            MojoExecutionException.class,
            () -> createMojo(targetDir, MissingDataSource.class, "fail").execute());

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "No constructor annotated with '%s' was found in class '%s'",
                DataSource.class.getName(), MissingDataSource.class.getName()));
  }

  @Test
  public void fail_missingConfig(@TempDir Path targetDir) {
    MojoExecutionException exception =
        assertThrows(
            MojoExecutionException.class,
            () -> createMojo(targetDir, MissingConfigDataSource.class, "fail").execute());

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "No constructor annotated with '%s' was found in class '%s'",
                Config.class.getName(), MissingConfig.class.getName()));
  }

  @Test
  public void fail_multipleDataSources(@TempDir Path targetDir) {
    MojoExecutionException exception =
        assertThrows(
            MojoExecutionException.class,
            () -> createMojo(targetDir, MultipleDataSource.class, "fail").execute());

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "Exactly one constructor should be annotated with '%s' in class '%s'",
                DataSource.class.getName(), MultipleDataSource.class.getName()));
  }

  @Test
  public void fail_missingParameterValue(@TempDir Path targetDir) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> createMojo(targetDir, ParamMissingValueDataSource.class, "fail").execute());

    assertThat(exception).hasMessageThat().startsWith("Missing parameter name");
  }

  private ConnectorGeneratorMojo createMojo(
      Path targetDir, Class<?> connectorClass, String protoFileName) throws URISyntaxException {
    String className = connectorClass.getName();
    URL resource =
        connectorClass.getClassLoader().getResource(className.replace('.', '/') + ".class");
    assertNotNull(resource);

    String path = Paths.get(resource.toURI()).toString();
    Path outputDir =
        Paths.get(path.substring(0, path.length() - className.length() - ".class".length()));

    return ConnectorGeneratorMojo.create(targetDir, outputDir, className, protoFileName);
  }
}
