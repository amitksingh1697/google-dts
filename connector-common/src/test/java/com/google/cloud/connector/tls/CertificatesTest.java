package com.google.cloud.connector.tls;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public final class CertificatesTest {
  @Test
  public void certificateSubjectCanonicalName_returnsNameFromCertificate() throws Exception {
    assertThat(Certificates.subjectCanonicalName(testCertificatePem()))
        .isEqualTo("GCDFS4QAS.sapqa.cloudsufi.com");
  }

  private static String testCertificatePem() throws IOException {
    try (var reader =
        new InputStreamReader(
            Objects.requireNonNull(
                CertificatesTest.class.getResourceAsStream(
                    "/server-certificate.pem")))) {
      return CharStreams.toString(reader);
    }
  }
}
