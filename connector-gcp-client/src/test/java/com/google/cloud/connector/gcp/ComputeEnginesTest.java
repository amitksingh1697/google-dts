package com.google.cloud.connector.gcp;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit tests for {@link ComputeEngines}. */
public class ComputeEnginesTest {

  @RegisterExtension
  private static final MockMetadataServer metadataServer = new MockMetadataServer();

  @Test
  public void isComputeEngine() {
    assertThat(ComputeEngines.isComputeEngine()).isTrue();
  }

  @Test
  public void getProjectId() throws IOException {
    assertThat(ComputeEngines.getProjectId()).isEqualTo("test-project");
  }

  @Test
  public void getProjectNumber() throws IOException {
    assertThat(ComputeEngines.getProjectNumber()).isEqualTo("1234567890");
  }

  @Test
  public void getHostname() throws IOException {
    assertThat(ComputeEngines.getHostname()).isEqualTo("test.test-project.internal");
  }

  @Test
  public void getInstanceName() throws IOException {
    assertThat(ComputeEngines.getInstanceName())
        .isEqualTo(
            "test-instance-name-with-special$?chars@(@and-longer-than-"
                + "sixty-three-chars-with-trailing-hyphens----");
  }

  @Test
  public void getZone() throws IOException {
    assertThat(ComputeEngines.getZone()).isEqualTo("us-west1-a");
  }

  @Test
  public void getRegion() throws IOException {
    assertThat(ComputeEngines.getRegion()).isEqualTo("us-west1");
  }

  @Test
  public void getIp() throws IOException {
    assertThat(ComputeEngines.getIpv4())
        .isEqualTo(Optional.of(InetAddress.getByName("192.168.1.1")));
  }

  @Test
  public void getIpv6() throws IOException {
    assertThat(ComputeEngines.getIpv6())
        .isEqualTo(Optional.of(InetAddress.getByName("2600:2d00:4230:9531:a80:12:0:0")));
  }

  @Test
  public void getNetwork() throws IOException {
    assertThat(ComputeEngines.getNetwork())
        .isEqualTo("projects/test-project/global/networks/default");
  }
}
