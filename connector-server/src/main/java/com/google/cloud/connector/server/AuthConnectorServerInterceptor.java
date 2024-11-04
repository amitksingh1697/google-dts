package com.google.cloud.connector.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.alts.AuthorizationUtil;
import java.util.List;

/**
 * Server interceptor to intercept requests for enforcing authorization.
 */
class AuthConnectorServerInterceptor implements ServerInterceptor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final List<String> serviceAccounts;

  AuthConnectorServerInterceptor(List<String> serviceAccounts) {
    this.serviceAccounts = ImmutableList.copyOf(serviceAccounts);
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {
    Status status = AuthorizationUtil.clientAuthorizationCheck(
        call, serviceAccounts);
    if (status.isOk()) {
      return next.startCall(call, headers);
    }
    logger.atInfo().log(
        "RPC authorization check failed with %s due to %s.",
        status.getCode(), status.getCause());
    call.close(status, headers);
    return new ServerCall.Listener<ReqT>() {};
  }
}
