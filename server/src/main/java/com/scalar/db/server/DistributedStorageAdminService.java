package com.scalar.db.server;

import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.rpc.CreateTableRequest;
import com.scalar.db.rpc.DistributedStorageAdminGrpc;
import com.scalar.db.rpc.DropTableRequest;
import com.scalar.db.rpc.GetTableMetadataRequest;
import com.scalar.db.rpc.GetTableMetadataResponse;
import com.scalar.db.rpc.GetTableMetadataResponse.Builder;
import com.scalar.db.rpc.TruncateTableRequest;
import com.scalar.db.util.ProtoUtil;
import com.scalar.db.util.ThrowableRunnable;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedStorageAdminService
    extends DistributedStorageAdminGrpc.DistributedStorageAdminImplBase {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(DistributedStorageAdminService.class);

  private final DistributedStorageAdmin admin;
  private final Metrics metrics;

  @Inject
  public DistributedStorageAdminService(DistributedStorageAdmin admin, Metrics metrics) {
    this.admin = admin;
    this.metrics = metrics;
  }

  @Override
  public void createTable(CreateTableRequest request, StreamObserver<Empty> responseObserver) {
    execute(
        () -> {
          admin.createTable(
              request.getNamespace(),
              request.getTable(),
              ProtoUtil.toTableMetadata(request.getTableMetadata()),
              request.getOptionsMap());
          responseObserver.onNext(Empty.getDefaultInstance());
          responseObserver.onCompleted();
        },
        responseObserver,
        "createTable");
  }

  @Override
  public void dropTable(DropTableRequest request, StreamObserver<Empty> responseObserver) {
    execute(
        () -> {
          admin.dropTable(request.getNamespace(), request.getTable());
          responseObserver.onNext(Empty.getDefaultInstance());
          responseObserver.onCompleted();
        },
        responseObserver,
        "dropTable");
  }

  @Override
  public void truncateTable(TruncateTableRequest request, StreamObserver<Empty> responseObserver) {
    execute(
        () -> {
          admin.truncateTable(request.getNamespace(), request.getTable());
          responseObserver.onNext(Empty.getDefaultInstance());
          responseObserver.onCompleted();
        },
        responseObserver,
        "truncateTable");
  }

  @Override
  public void getTableMetadata(
      GetTableMetadataRequest request, StreamObserver<GetTableMetadataResponse> responseObserver) {
    execute(
        () -> {
          TableMetadata tableMetadata =
              admin.getTableMetadata(request.getNamespace(), request.getTable());
          Builder builder = GetTableMetadataResponse.newBuilder();
          if (tableMetadata != null) {
            builder.setTableMetadata(ProtoUtil.toTableMetadata(tableMetadata));
          }
          responseObserver.onNext(builder.build());
          responseObserver.onCompleted();
        },
        responseObserver,
        "getTableMetadata");
  }

  private void execute(
      ThrowableRunnable<Throwable> runnable, StreamObserver<?> responseObserver, String method) {
    try {
      metrics.measure(DistributedStorageAdminService.class, method, runnable);
    } catch (IllegalArgumentException | IllegalStateException e) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (Throwable t) {
      LOGGER.error("an internal error happened during the execution", t);
      responseObserver.onError(
          Status.INTERNAL.withDescription(t.getMessage()).asRuntimeException());
      if (t instanceof Error) {
        throw (Error) t;
      }
    }
  }
}
