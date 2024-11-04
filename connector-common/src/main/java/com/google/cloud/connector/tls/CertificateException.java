package com.google.cloud.connector.tls;

/** An exception thrown when an operation on certificate fails. */
public final class CertificateException extends RuntimeException {

  /** Constructs the exception with a message and a cause. */
  public CertificateException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Constructs the exception with just a message. */
  public CertificateException(String message) {
    super(message);
  }
}
