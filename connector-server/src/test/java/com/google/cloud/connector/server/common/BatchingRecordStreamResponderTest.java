package com.google.cloud.connector.server.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.federation.v1alpha1.Data;
import com.google.cloud.connector.api.RecordReader;
import com.google.cloud.connector.api.data.RecordBuilder;
import com.google.cloud.connector.api.schema.SchemaBuilder;
import com.google.cloud.connector.server.ConnectorDataSchemaBuilder;
import com.google.cloud.connector.server.metrics.DataBatchMetricsRecorder;
import com.google.common.collect.ImmutableList;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.OpenTelemetry;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchingRecordStreamResponderTest {
  Random random = new Random();
  BatchingRecordStreamResponder responder;
  RecordReader recordReader;

  @BeforeEach
  public void before() {
    recordReader = mock(RecordReader.class);
    responder = spy(BatchingRecordStreamResponder.class);

    doAnswer(
            (a) -> {
              SchemaBuilder builder = a.getArgument(0, SchemaBuilder.class);
              builder.field("bytes").typeBytes();
              builder.endStruct();
              return null;
            })
        .when(recordReader)
        .getRecordSchema(any(SchemaBuilder.class));
  }

  @Test
  void sendRecordsInBatches_singleBatch() throws Exception {
    when(recordReader.nextRecord(any()))
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[63_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[63_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[63_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[63_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        .thenAnswer((a) -> false);

    FakeStreamObserver observer = new FakeStreamObserver(new CompletableFuture<>());
    responder.sendRecordsInBatches(
        observer,
        recordReader,
        new ConnectorDataSchemaBuilder(),
        new DataBatchMetricsRecorder(OpenTelemetry.noop(), "datasource", "apiName", Instant.now()));
    ImmutableList<Data> batches = observer.getListFuture().get();
    assertThat(batches.size()).isEqualTo(1);
    Data firstBatch = batches.get(0);
    assertTrue(firstBatch.hasSchema());
    assertTrue(firstBatch.hasDataBlock());
  }

  @Test
  void sendRecordsInBatches_multipleBatches() throws Exception {
    when(recordReader.nextRecord(any()))
        // First batch
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[257_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        // Second batch
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[100_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[200_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        // Third batch
        .thenAnswer(
            (a) -> {
              RecordBuilder builder = a.getArgument(0, RecordBuilder.class);
              byte[] bytes = new byte[200_000];
              random.nextBytes(bytes);
              builder.field("bytes").set(bytes).endStruct();
              return true;
            })
        .thenAnswer((a) -> false);

    FakeStreamObserver observer = new FakeStreamObserver(new CompletableFuture<>());
    responder.sendRecordsInBatches(
        observer,
        recordReader,
        new ConnectorDataSchemaBuilder(),
        new DataBatchMetricsRecorder(OpenTelemetry.noop(), "datasource", "apiName", Instant.now()));
    ImmutableList<Data> batches = observer.getListFuture().get();
    assertThat(batches.size()).isEqualTo(3);
    for (int i = 0; i < batches.size(); i++) {
      if (i == 0) {
        assertTrue(batches.get(i).hasSchema());
      } else {
        assertFalse(batches.get(i).hasSchema());
      }
      assertTrue(batches.get(i).hasDataBlock());
    }
  }

  @Test
  void sendRecordsInBatches_noBatches() throws Exception {
    when(recordReader.nextRecord(any())).thenAnswer((a) -> false);

    FakeStreamObserver observer = new FakeStreamObserver(new CompletableFuture<>());
    responder.sendRecordsInBatches(
        observer,
        recordReader,
        new ConnectorDataSchemaBuilder(),
        new DataBatchMetricsRecorder(OpenTelemetry.noop(), "datasource", "apiName", Instant.now()));
    ImmutableList<Data> batches = observer.getListFuture().get();
    assertThat(batches.size()).isEqualTo(1);
    Data firstBatch = batches.get(0);
    assertTrue(firstBatch.hasSchema());
    assertFalse(firstBatch.hasDataBlock());
  }

  // Fake stream observer to check the results returned
  private static final class FakeStreamObserver implements StreamObserver<Data> {
    private final CompletableFuture<ImmutableList<Data>> listFuture;
    private final ImmutableList.Builder<Data> result;

    private FakeStreamObserver(CompletableFuture<ImmutableList<Data>> listFuture) {
      this.listFuture = listFuture;
      this.result = ImmutableList.builder();
    }

    @Override
    public void onNext(Data data) {
      result.add(data);
    }

    @Override
    public void onError(Throwable t) {
      listFuture.completeExceptionally(t);
    }

    @Override
    public void onCompleted() {
      if (listFuture.isDone()) {
        throw new RuntimeException("on complete should only be called once");
      }
      listFuture.complete(result.build());
    }

    public CompletableFuture<ImmutableList<Data>> getListFuture() {
      return listFuture;
    }
  }
}
