package com.google.cloud.connector.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.cloud.connector.api.exception.ConnectorException.FailureReason;
import org.junit.jupiter.api.Test;

class ConnectorExceptionTest {
  @Test
  public void connectorException_invalidArgument() {
    Throwable t = new RuntimeException("oof");
    ConnectorException.Builder builder = ConnectorException.newBuilder("unexpected exception", t);
    builder.setFailureReason(FailureReason.INVALID_ARGUMENT);

    // Check exception getters
    ConnectorException exception = builder.build();
    assertEquals("unexpected exception", exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception.getCause());
    assertEquals(FailureReason.INVALID_ARGUMENT, exception.getFailureReason());
  }

  @Test
  public void connectorException_permissionDenied() {
    Throwable t = new RuntimeException("oof");
    ConnectorException.Builder builder = ConnectorException.newBuilder("unexpected exception", t);
    builder.setFailureReason(FailureReason.PERMISSION_DENIED);

    // Check exception getters
    ConnectorException exception = builder.build();
    assertEquals("unexpected exception", exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception.getCause());
    assertEquals(FailureReason.PERMISSION_DENIED, exception.getFailureReason());
  }

  @Test
  public void connectorException_serviceUnavailable() {
    Throwable t = new RuntimeException("oof");
    ConnectorException.Builder builder = ConnectorException.newBuilder("unexpected exception", t);
    builder.setFailureReason(FailureReason.SERVICE_UNAVAILABLE);

    // Check exception getters
    ConnectorException exception = builder.build();
    assertEquals("unexpected exception", exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception.getCause());
    assertEquals(FailureReason.SERVICE_UNAVAILABLE, exception.getFailureReason());
  }

  @Test
  public void connectorException_unauthenticated() {
    Throwable t = new RuntimeException("oof");
    ConnectorException.Builder builder = ConnectorException.newBuilder("unexpected exception", t);
    builder.setFailureReason(FailureReason.UNAUTHENTICATED);

    // Check exception getters
    ConnectorException exception = builder.build();
    assertEquals("unexpected exception", exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception.getCause());
    assertEquals(FailureReason.UNAUTHENTICATED, exception.getFailureReason());
  }

  @Test
  public void connectorException_internal() {
    Throwable t = new RuntimeException("oof");
    ConnectorException.Builder builder = ConnectorException.newBuilder("unexpected exception", t);
    builder.setFailureReason(FailureReason.INTERNAL);

    // Check exception getters
    ConnectorException exception = builder.build();
    assertEquals("unexpected exception", exception.getMessage());
    assertInstanceOf(RuntimeException.class, exception.getCause());
    assertEquals(FailureReason.INTERNAL, exception.getFailureReason());
  }
}
