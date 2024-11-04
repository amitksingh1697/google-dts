package com.google.cloud.connector.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/** Utilities for working with TLS certificates. */
public final class Certificates {
  /**
   * Extracts the canonical name (CN; typically that's the host name) of the certificate subject
   * from the certificate.
   */
  public static String subjectCanonicalName(String certificatePem) {
    X509Certificate certificate = certificateFromPem(certificatePem);
    try {
      return new LdapName(certificate.getSubjectX500Principal().getName())
          .getRdns().stream()
              .filter(rdn -> rdn.getType().equalsIgnoreCase("cn"))
              .map(rdn -> rdn.getValue().toString())
              .findFirst()
              .orElseThrow(
                  () -> new CertificateException("No subject CN found in the certificate"));
    } catch (InvalidNameException e) {
      throw new CertificateException("Unable to extract subject CN from the certificate", e);
    }
  }

  private static X509Certificate certificateFromPem(String certificatePem) {
    try (var in =
        new ByteArrayInputStream(certificatePem.getBytes(StandardCharsets.UTF_8))) {
      return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
    } catch (IOException | java.security.cert.CertificateException e) {
      throw new CertificateException("Cannot parse X.509 certificate from PEM", e);
    }
  }

  private Certificates() {}
}
