package com.google.cloud.connector.api.exception;

/**
 * Exception class used by Connector implementations to relay information about failures and how
 * they should be handled.
 */
public class ConnectorException extends RuntimeException {

  private final FailureReason failureReason;

  private ConnectorException(String message, Throwable cause, FailureReason reason) {
    super(message, cause);
    this.failureReason = reason;
  }

  public FailureReason getFailureReason() {
    return failureReason;
  }

  /** Enumeration containing all possible failure reasons for a connector exception. */
  public enum FailureReason {
    /** The supplied configuration or query is invalid. */
    INVALID_ARGUMENT,
    /** Credentials are invalid. */
    PERMISSION_DENIED,
    /** Authentication is required. */
    UNAUTHENTICATED,
    /** Service is temporarily not able to handle the request. */
    SERVICE_UNAVAILABLE,
    /** A resource is not found. */
    NOT_FOUND,
    /** Case of throttling/downtime due to resource exhaustion. */
    RESOURCE_EXHAUSTED,
    /** Some error occurred at datasource (like a generic 500 status code). */
    UNKNOWN,
    /** Some error occurred at the datasource due to preconditions not setup properly.*/
    FAILED_PRECONDITION,
    /** Something else caused the connector to fail. */
    INTERNAL;

    FailureReason() {
      // no-op
    }
  }

  public static Builder newBuilder(String message, Throwable cause) {
    return new Builder(message, cause);
  }

  public static Builder newBuilder(String message) {
    return new Builder(message, null);
  }

  /** Builder class for {@link ConnectorException}. */
  public static final class Builder {

    private final String message;
    private final Throwable cause;
    private FailureReason failureReason;

    /**
     * Constructor.
     *
     * @param message Error message
     * @param cause Failure cause
     */
    public Builder(String message, Throwable cause) {
      this.message = message;
      this.cause = cause;
      this.failureReason = FailureReason.INTERNAL;
    }

    public Builder setFailureReason(FailureReason failureReason) {
      this.failureReason = failureReason;
      return this;
    }

    public ConnectorException build() {
      return new ConnectorException(message, cause, failureReason);
    }
  }
}