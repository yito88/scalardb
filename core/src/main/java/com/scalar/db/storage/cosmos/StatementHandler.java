package com.scalar.db.storage.cosmos;

import static com.google.common.base.Preconditions.checkNotNull;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.scalar.db.api.Operation;
import com.scalar.db.exception.storage.ExecutionException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A handler class for statements */
@ThreadSafe
public abstract class StatementHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(StatementHandler.class);
  protected final CosmosClient client;
  protected final CosmosTableMetadataManager metadataManager;

  /**
   * Constructs a {@code StatementHandler} with the specified {@link CosmosClient}
   *
   * @param client {@code CosmosClient}
   * @param metadataManager {@code TableMetadataManager}
   */
  protected StatementHandler(CosmosClient client, CosmosTableMetadataManager metadataManager) {
    this.client = checkNotNull(client);
    this.metadataManager = checkNotNull(metadataManager);
  }

  /**
   * Executes the specified {@code Operation}
   *
   * @param operation an {@code Operation} to execute
   * @return a {@code ResultSet}
   * @throws ExecutionException if the execution failed
   */
  @Nonnull
  public List<Record> handle(Operation operation) throws ExecutionException {
    try {
      List<Record> results = execute(operation);

      return results;
    } catch (RuntimeException e) {
      LOGGER.error(e.getMessage());
      throw new ExecutionException(e.getMessage(), e);
    }
  }

  protected abstract List<Record> execute(Operation operation) throws CosmosException;

  @Nonnull
  protected CosmosContainer getContainer(Operation operation) {
    return client
        .getDatabase(operation.forFullNamespace().get())
        .getContainer(operation.forTable().get());
  }
}
