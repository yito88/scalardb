package com.scalar.db.transaction.rpc;

import com.google.common.util.concurrent.Uninterruptibles;
import com.scalar.db.api.Get;
import com.scalar.db.api.Mutation;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CommitConflictException;
import com.scalar.db.exception.transaction.CommitException;
import com.scalar.db.exception.transaction.CrudConflictException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.exception.transaction.UnknownTransactionStatusException;
import com.scalar.db.rpc.DistributedTransactionGrpc.DistributedTransactionStub;
import com.scalar.db.rpc.TransactionRequest;
import com.scalar.db.rpc.TransactionRequest.AbortRequest;
import com.scalar.db.rpc.TransactionRequest.CommitRequest;
import com.scalar.db.rpc.TransactionRequest.GetRequest;
import com.scalar.db.rpc.TransactionRequest.MutateRequest;
import com.scalar.db.rpc.TransactionRequest.ScanRequest;
import com.scalar.db.rpc.TransactionRequest.StartRequest;
import com.scalar.db.rpc.TransactionResponse;
import com.scalar.db.rpc.TransactionResponse.GetResponse;
import com.scalar.db.storage.rpc.GrpcConfig;
import com.scalar.db.storage.rpc.GrpcTableMetadataManager;
import com.scalar.db.util.ProtoUtil;
import com.scalar.db.util.Utility;
import com.scalar.db.util.retry.ServiceTemporaryUnavailableException;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class GrpcTransactionOnBidirectionalStream
    implements ClientResponseObserver<TransactionRequest, TransactionResponse> {

  private final GrpcConfig config;
  private final GrpcTableMetadataManager metadataManager;
  private final BlockingQueue<ResponseOrError> queue = new LinkedBlockingQueue<>();
  private final AtomicBoolean finished = new AtomicBoolean();

  private ClientCallStreamObserver<TransactionRequest> requestStream;

  public GrpcTransactionOnBidirectionalStream(
      GrpcConfig config,
      DistributedTransactionStub stub,
      GrpcTableMetadataManager metadataManager) {
    this.config = config;
    this.metadataManager = metadataManager;
    stub.transaction(this);
  }

  @Override
  public void beforeStart(ClientCallStreamObserver<TransactionRequest> requestStream) {
    this.requestStream = requestStream;
  }

  @Override
  public void onNext(TransactionResponse response) {
    Uninterruptibles.putUninterruptibly(queue, new ResponseOrError(response));
  }

  @Override
  public void onError(Throwable t) {
    Uninterruptibles.putUninterruptibly(queue, new ResponseOrError(t));
  }

  @Override
  public void onCompleted() {
    requestStream.onCompleted();
  }

  private ResponseOrError sendRequest(TransactionRequest request) {
    requestStream.onNext(request);

    ResponseOrError responseOrError =
        Utility.pollUninterruptibly(
            queue, config.getDeadlineDurationMillis(), TimeUnit.MILLISECONDS);
    if (responseOrError == null) {
      requestStream.cancel("deadline exceeded", null);

      // Should receive a CANCELED error
      return Uninterruptibles.takeUninterruptibly(queue);
    }
    return responseOrError;
  }

  private void throwIfTransactionFinished() {
    if (finished.get()) {
      throw new IllegalStateException("the transaction is finished");
    }
  }

  public String startTransaction() throws TransactionException {
    return startTransaction(null);
  }

  public String startTransaction(@Nullable String transactionId) throws TransactionException {
    throwIfTransactionFinished();

    StartRequest request;
    if (transactionId == null) {
      request = StartRequest.getDefaultInstance();
    } else {
      request = StartRequest.newBuilder().setTransactionId(transactionId).build();
    }
    ResponseOrError responseOrError =
        sendRequest(TransactionRequest.newBuilder().setStartRequest(request).build());
    throwIfErrorForStart(responseOrError);
    return responseOrError.getResponse().getStartResponse().getTransactionId();
  }

  private void throwIfErrorForStart(ResponseOrError responseOrError) throws TransactionException {
    if (responseOrError.isError()) {
      finished.set(true);
      Throwable error = responseOrError.getError();
      if (error instanceof StatusRuntimeException) {
        StatusRuntimeException e = (StatusRuntimeException) error;
        if (e.getStatus().getCode() == Code.INVALID_ARGUMENT) {
          throw new IllegalArgumentException(e.getMessage(), e);
        }
        if (e.getStatus().getCode() == Code.UNAVAILABLE) {
          throw new ServiceTemporaryUnavailableException(e.getMessage(), e);
        }
      }
      if (error instanceof Error) {
        throw (Error) error;
      }
      throw new TransactionException("failed to start", error);
    }
  }

  public Optional<Result> get(Get get) throws CrudException {
    throwIfTransactionFinished();

    ResponseOrError responseOrError =
        sendRequest(
            TransactionRequest.newBuilder()
                .setGetRequest(GetRequest.newBuilder().setGet(ProtoUtil.toGet(get)))
                .build());
    throwIfErrorForCrud(responseOrError);

    GetResponse getResponse = responseOrError.getResponse().getGetResponse();
    if (getResponse.hasResult()) {
      TableMetadata tableMetadata = metadataManager.getTableMetadata(get);
      return Optional.of(ProtoUtil.toResult(getResponse.getResult(), tableMetadata));
    }

    return Optional.empty();
  }

  public List<Result> scan(Scan scan) throws CrudException {
    throwIfTransactionFinished();

    ResponseOrError responseOrError =
        sendRequest(
            TransactionRequest.newBuilder()
                .setScanRequest(ScanRequest.newBuilder().setScan(ProtoUtil.toScan(scan)))
                .build());
    throwIfErrorForCrud(responseOrError);

    TableMetadata tableMetadata = metadataManager.getTableMetadata(scan);
    return responseOrError.getResponse().getScanResponse().getResultList().stream()
        .map(r -> ProtoUtil.toResult(r, tableMetadata))
        .collect(Collectors.toList());
  }

  public void mutate(Mutation mutation) throws CrudException {
    throwIfTransactionFinished();

    ResponseOrError responseOrError =
        sendRequest(
            TransactionRequest.newBuilder()
                .setMutateRequest(
                    MutateRequest.newBuilder().addMutation(ProtoUtil.toMutation(mutation)))
                .build());
    throwIfErrorForCrud(responseOrError);
  }

  public void mutate(List<? extends Mutation> mutations) throws CrudException {
    throwIfTransactionFinished();

    MutateRequest.Builder builder = MutateRequest.newBuilder();
    mutations.forEach(m -> builder.addMutation(ProtoUtil.toMutation(m)));
    ResponseOrError responseOrError =
        sendRequest(TransactionRequest.newBuilder().setMutateRequest(builder).build());
    throwIfErrorForCrud(responseOrError);
  }

  private void throwIfErrorForCrud(ResponseOrError responseOrError) throws CrudException {
    if (responseOrError.isError()) {
      finished.set(true);
      Throwable error = responseOrError.getError();
      if (error instanceof Error) {
        throw (Error) error;
      }
      throw new CrudException("failed to execute crud", error);
    }

    TransactionResponse response = responseOrError.getResponse();
    if (response.hasError()) {
      switch (response.getError().getErrorCode()) {
        case INVALID_ARGUMENT:
          throw new IllegalArgumentException(response.getError().getMessage());
        case CONFLICT:
          throw new CrudConflictException(response.getError().getMessage());
        default:
          throw new CrudException(response.getError().getMessage());
      }
    }
  }

  public void commit() throws CommitException, UnknownTransactionStatusException {
    throwIfTransactionFinished();

    ResponseOrError responseOrError =
        sendRequest(
            TransactionRequest.newBuilder()
                .setCommitRequest(CommitRequest.getDefaultInstance())
                .build());
    finished.set(true);
    throwIfErrorForCommit(responseOrError);
  }

  private void throwIfErrorForCommit(ResponseOrError responseOrError)
      throws CommitException, UnknownTransactionStatusException {
    if (responseOrError.isError()) {
      Throwable error = responseOrError.getError();
      if (error instanceof Error) {
        throw (Error) error;
      }
      throw new CommitException("failed to commit", error);
    }

    if (responseOrError.getResponse().hasError()) {
      switch (responseOrError.getResponse().getError().getErrorCode()) {
        case CONFLICT:
          throw new CommitConflictException(responseOrError.getResponse().getError().getMessage());
        case UNKNOWN_TRANSACTION:
          throw new UnknownTransactionStatusException(
              responseOrError.getResponse().getError().getMessage());
        default:
          throw new CommitException(responseOrError.getResponse().getError().getMessage());
      }
    }
  }

  public void abort() throws AbortException {
    if (finished.get()) {
      return;
    }

    ResponseOrError responseOrError =
        sendRequest(
            TransactionRequest.newBuilder()
                .setAbortRequest(AbortRequest.getDefaultInstance())
                .build());
    finished.set(true);
    throwIfErrorForAbort(responseOrError);
  }

  private void throwIfErrorForAbort(ResponseOrError responseOrError) throws AbortException {
    if (responseOrError.isError()) {
      Throwable error = responseOrError.getError();
      if (error instanceof Error) {
        throw (Error) error;
      }
      throw new AbortException("failed to abort", error);
    }

    TransactionResponse response = responseOrError.getResponse();
    if (response.hasError()) {
      throw new AbortException(response.getError().getMessage());
    }
  }

  private static class ResponseOrError {
    private final TransactionResponse response;
    private final Throwable error;

    public ResponseOrError(TransactionResponse response) {
      this.response = response;
      this.error = null;
    }

    public ResponseOrError(Throwable error) {
      this.response = null;
      this.error = error;
    }

    private boolean isError() {
      return error != null;
    }

    public TransactionResponse getResponse() {
      return response;
    }

    public Throwable getError() {
      return error;
    }
  }
}
