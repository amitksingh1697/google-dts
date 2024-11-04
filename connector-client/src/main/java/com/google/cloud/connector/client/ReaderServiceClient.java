package com.google.cloud.connector.client;

import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.bigquery.federation.v1alpha1.ReadStreamRequest;
import com.google.cloud.bigquery.federation.v1alpha1.ReaderServiceGrpc;
import com.google.protobuf.Struct;
import io.grpc.ManagedChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Client implementation which uses {@link ReaderServiceGrpc} to interact with the Reader Service.
 *
 * <p>Implements {@link AutoCloseable} to manage GRPC channel lifecycle.
 */
public class ReaderServiceClient implements AutoCloseable {
  private final Struct connectorParameters;
  private final ManagedChannel channel;

  /**
   * Constructor.
   *
   * @param channel GRPC managed channel
   * @param connectorParameters {@link Struct} containing the datasource-type-specific parameters
   *     for this connector
   */
  public ReaderServiceClient(ManagedChannel channel, Struct connectorParameters) {
    this.channel = channel;
    this.connectorParameters = connectorParameters;
  }

  /**
   * Read all {@link Data} entries for a specified Stream.
   *
   * @param stream stream to read {@link Data} pages from
   * @return Iterator for all {@link Data} pages in this stream
   */
  public Iterator<Data> readStream(String stream) {
    ReadStreamRequest request =
        ReadStreamRequest.newBuilder()
            .setResultStream(stream)
            .setParameters(connectorParameters)
            .build();

    ReaderServiceGrpc.ReaderServiceBlockingStub stub = ReaderServiceGrpc.newBlockingStub(channel);
    return stub.readStream(request);
  }

  @Override
  public void close() {
    // Close the GRPC channel after completion.
    try {
      channel.shutdown();
      channel.awaitTermination(5, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted Exception when waiting for GRPC channel to close", e);
    }
  }
}