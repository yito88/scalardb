package com.scalar.db.transaction.consensuscommit;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Isolation;
import com.scalar.db.api.TransactionState;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.transaction.CoordinatorException;
import com.scalar.db.exception.transaction.UnknownTransactionStatusException;
import com.scalar.db.transaction.consensuscommit.Coordinator.State;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class ConsensusCommitManager implements DistributedTransactionManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsensusCommitManager.class);
  private final DistributedStorage storage;
  private final DatabaseConfig config;
  private Coordinator coordinator;
  private RecoveryHandler recovery;
  private CommitHandler commit;
  private Optional<String> namespace;
  private Optional<String> tableName;

  @Inject
  public ConsensusCommitManager(DistributedStorage storage, DatabaseConfig config) {
    this.storage = storage;
    this.config = config;
    this.coordinator = new Coordinator(storage);
    this.recovery = new RecoveryHandler(storage, coordinator);
    this.commit = new CommitHandler(storage, coordinator, recovery);
    this.namespace = storage.getNamespace();
    this.tableName = storage.getTable();
  }

  @VisibleForTesting
  public ConsensusCommitManager(
      DistributedStorage storage,
      DatabaseConfig config,
      Coordinator coordinator,
      RecoveryHandler recovery,
      CommitHandler commit) {
    this.storage = storage;
    this.config = config;
    this.coordinator = coordinator;
    this.recovery = recovery;
    this.commit = commit;
    this.namespace = storage.getNamespace();
    this.tableName = storage.getTable();
  }

  @Override
  public void with(String namespace, String tableName) {
    this.namespace = Optional.ofNullable(namespace);
    this.tableName = Optional.ofNullable(tableName);
  }

  @Override
  public void withNamespace(String namespace) {
    this.namespace = Optional.ofNullable(namespace);
  }

  @Override
  public Optional<String> getNamespace() {
    return namespace;
  }

  @Override
  public void withTable(String tableName) {
    this.tableName = Optional.ofNullable(tableName);
  }

  @Override
  public Optional<String> getTable() {
    return tableName;
  }

  @Override
  public ConsensusCommit start() {
    return start(config.getIsolation(), config.getSerializableStrategy());
  }

  @Override
  public ConsensusCommit start(String txId) {
    return start(txId, config.getIsolation(), config.getSerializableStrategy());
  }

  @Deprecated
  @Override
  public synchronized ConsensusCommit start(Isolation isolation) {
    return start(isolation, config.getSerializableStrategy());
  }

  @Deprecated
  @Override
  public synchronized ConsensusCommit start(String txId, Isolation isolation) {
    return start(txId, isolation, config.getSerializableStrategy());
  }

  @Deprecated
  @Override
  public synchronized ConsensusCommit start(
      Isolation isolation, com.scalar.db.api.SerializableStrategy strategy) {
    String txId = UUID.randomUUID().toString();
    return start(txId, isolation, strategy);
  }

  @Deprecated
  @Override
  public synchronized ConsensusCommit start(com.scalar.db.api.SerializableStrategy strategy) {
    String txId = UUID.randomUUID().toString();
    return start(txId, Isolation.SERIALIZABLE, strategy);
  }

  @Deprecated
  @Override
  public synchronized ConsensusCommit start(
      String txId, com.scalar.db.api.SerializableStrategy strategy) {
    return start(txId, Isolation.SERIALIZABLE, strategy);
  }

  @Override
  public synchronized ConsensusCommit start(
      String txId, Isolation isolation, com.scalar.db.api.SerializableStrategy strategy) {
    checkArgument(!Strings.isNullOrEmpty(txId));
    checkArgument(isolation != null);
    if (!config.getIsolation().equals(isolation)
        || !config.getSerializableStrategy().equals(strategy)) {
      LOGGER.warn(
          "Setting different isolation level or serializable strategy from the ones"
              + "in DatabaseConfig might cause unexpected anomalies.");
    }
    Snapshot snapshot = new Snapshot(txId, isolation, (SerializableStrategy) strategy);
    CrudHandler crud = new CrudHandler(storage, snapshot);
    ConsensusCommit consensus = new ConsensusCommit(crud, commit, recovery);
    namespace.ifPresent(n -> consensus.withNamespace(n));
    tableName.ifPresent(t -> consensus.withTable(t));
    return consensus;
  }

  @Override
  public TransactionState getState(String txId) {
    checkArgument(!Strings.isNullOrEmpty(txId));
    try {
      Optional<State> state = coordinator.getState(txId);
      if (state.isPresent()) {
        return state.get().getState();
      }
    } catch (CoordinatorException e) {
      // ignore
    }
    // Either no state exists or the exception is thrown
    return TransactionState.UNKNOWN;
  }

  @Override
  public TransactionState abort(String txId) {
    checkArgument(!Strings.isNullOrEmpty(txId));
    try {
      return commit.abort(txId);
    } catch (UnknownTransactionStatusException ignored) {
      return TransactionState.UNKNOWN;
    }
  }

  @Override
  public void close() {
    storage.close();
  }
}
