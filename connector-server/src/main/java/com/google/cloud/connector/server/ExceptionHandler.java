package com.google.cloud.connector.server;

import com.google.cloud.connector.api.exception.ConnectorException;
import com.google.cloud.connector.api.exception.ConnectorException.FailureReason;
import com.google.common.base.Throwables;
import com.google.common.flogger.FluentLogger;
import com.google.rpc.Code;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** A {@link ServerInterceptor} to handle server side exceptions in a unified way. */
public class ExceptionHandler implements ServerInterceptor {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String IS_RETRYABLE_METADATA_KEY = "isRetryable";
  private static final String CONNECTOR_FAILURE_REASON_METADATA_KEY = "failureReason";
  private static final String CONNECTOR_IMPL_ERROR_REASON = "CONNECTOR_IMPL";

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    Listener<ReqT> listener = next.startCall(call, headers);
    return new ExceptionCallListener<>(call, listener);
  }

  /**
   * A {@link Listener} to receive callback on different phases of a gRPC call in order to handle
   * exceptions raised by the gRPC service implementation.
   *
   * @param <ReqT> the request type
   * @param <RespT> the response type
   */
  private static final class ExceptionCallListener<ReqT, RespT>
      extends SimpleForwardingServerCallListener<ReqT> {

    private final ServerCall<ReqT, RespT> call;

    ExceptionCallListener(ServerCall<ReqT, RespT> call, Listener<ReqT> delegate) {
      super(delegate);
      this.call = call;
    }

    @Override
    public void onMessage(ReqT message) {
      try {
        super.onMessage(message);
      } catch (Exception e) {
        handleException(e);
        throw e;
      }
    }

    @Override
    public void onHalfClose() {
      try {
        super.onHalfClose();
      } catch (Exception e) {
        handleException(e);
        throw e;
      }
    }

    @Override
    public void onCancel() {
      try {
        super.onCancel();
      } catch (Exception e) {
        handleException(e);
        throw e;
      }
    }

    @Override
    public void onComplete() {
      try {
        super.onComplete();
      } catch (Exception e) {
        handleException(e);
        throw e;
      }
    }

    @Override
    public void onReady() {
      try {
        super.onReady();
      } catch (Exception e) {
        handleException(e);
        throw e;
      }
    }

    private void handleException(Exception ex) {
      Metadata trailers = new Metadata();

      // Log client error at fine, system error at severe
      if (ex instanceof IllegalArgumentException) {
        logger.atFine().withCause(ex).log("Invalid argument from client");
        call.close(
            Status.INVALID_ARGUMENT.withCause(ex).withDescription(ex.getMessage()), trailers);
        return;
      }

      // Log validation errors thrown by CDAP-based connectors
      if (ex instanceof ValidationException) {
        logger.atSevere().withCause(ex).log("Validation exception thrown by a connector");
        call.close(
            Status.INVALID_ARGUMENT.withCause(ex).withDescription(ex.getMessage()), trailers);
        return;
      }

      if (ex instanceof UncheckedIOException) {
        IOException cause = (IOException) ex.getCause();
        if (cause instanceof FileNotFoundException || cause instanceof NoSuchFileException) {
          logger.atFine().withCause(ex).log("Not found error");
          call.close(
              Status.NOT_FOUND.withCause(cause).withDescription(cause.getMessage()), trailers);
          return;
        }
      }

      // Add handler for Connector Exceptions.
      if (ex instanceof ConnectorException ce) {
        logger.atSevere().withCause(ce).log("Call failed with a ConnectorException");

        // Build RPC status with additional error information
        com.google.rpc.Status rpcStatus =
            com.google.rpc.Status.newBuilder()
                .setCode(getGrpcStatusCode(ce.getFailureReason()).getNumber())
                .setMessage(ce.getMessage())
                .build();

        // Return RPC status as a status runtime exception.
        StatusRuntimeException sre = StatusProto.toStatusRuntimeException(rpcStatus);
        call.close(sre.getStatus(), sre.getTrailers());
        return;
      }

      // If the service throws the StatusRuntimeException, just respond with it.
      if (ex instanceof StatusRuntimeException statusEx) {
        logger.atSevere().withCause(ex).log("Call failed with an exception");
        call.close(
            statusEx.getStatus(), Optional.ofNullable(statusEx.getTrailers()).orElse(trailers));
        return;
      }

      logger.atSevere().withCause(ex).log("Unexpected internal error");
      call.close(Status.INTERNAL.withCause(ex).withDescription(ex.getMessage()), trailers);
    }
  }

  private static Code getGrpcStatusCode(FailureReason reason) {
    return switch (reason) {
      case INVALID_ARGUMENT -> Code.INVALID_ARGUMENT;
      case NOT_FOUND -> Code.NOT_FOUND;
      case PERMISSION_DENIED -> Code.PERMISSION_DENIED;
      case RESOURCE_EXHAUSTED -> Code.RESOURCE_EXHAUSTED;
      case SERVICE_UNAVAILABLE -> Code.UNAVAILABLE;
      case UNAUTHENTICATED -> Code.UNAUTHENTICATED;
      case UNKNOWN -> Code.UNKNOWN;
      case FAILED_PRECONDITION -> Code.FAILED_PRECONDITION;
      default -> Code.INTERNAL;
    };
  }

  private static List<String> getStackTraceEntries(Throwable t) {
    return Arrays.asList(Throwables.getStackTraceAsString(t).split("\n"));
  }
}
