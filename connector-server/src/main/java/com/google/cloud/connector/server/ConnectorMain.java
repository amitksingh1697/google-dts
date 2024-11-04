package com.google.cloud.connector.server;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/** The main class for running the Connector gRPC server. */
@Command(usageHelpAutoWidth = true)
public class ConnectorMain implements Callable<Integer> {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Mixin private ConnectorModule connectorModule;

  /** The main entry point to run the connector server as a standalone application. */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new ConnectorMain()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    Injector injector = Guice.createInjector(connectorModule);

    logger.atInfo().log("Starting connector server");
    // Start the connector gRPC server and block the main thread until receiving request for
    // termination.
    ConnectorServer server = injector.getInstance(ConnectorServer.class);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    server.stopAsync().awaitTerminated();
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                }));

    server.startAsync().awaitRunning();
    logger.atInfo().log("Connector server started at port %d", server.getPort());
    server.awaitTerminated();

    return 0;
  }
}
