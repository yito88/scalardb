package com.scalar.db.transaction.jdbc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Isolation;
import com.scalar.db.api.SerializableStrategy;
import com.scalar.db.api.TransactionState;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.storage.common.checker.OperationChecker;
import com.scalar.db.storage.jdbc.JdbcConfig;
import com.scalar.db.storage.jdbc.JdbcService;
import com.scalar.db.storage.jdbc.JdbcTableMetadataManager;
import com.scalar.db.storage.jdbc.JdbcUtils;
import com.scalar.db.storage.jdbc.RdbEngine;
import com.scalar.db.storage.jdbc.query.QueryBuilder;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class JdbcTransactionManager implements DistributedTransactionManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTransactionManager.class);

  private final BasicDataSource dataSource;
  private final RdbEngine rdbEngine;
  private final JdbcService jdbcService;
  private Optional<String> namespace;
  private Optional<String> tableName;

  @Inject
  public JdbcTransactionManager(JdbcConfig config) {
    dataSource = JdbcUtils.initDataSource(config, true);
    Optional<String> namespacePrefix = config.getNamespacePrefix();
    rdbEngine = JdbcUtils.getRdbEngine(config.getContactPoints().get(0));
    JdbcTableMetadataManager tableMetadataManager =
        new JdbcTableMetadataManager(dataSource, namespacePrefix, rdbEngine);
    OperationChecker operationChecker = new OperationChecker(tableMetadataManager);
    QueryBuilder queryBuilder = new QueryBuilder(tableMetadataManager, rdbEngine);
    jdbcService =
        new JdbcService(tableMetadataManager, operationChecker, queryBuilder, namespacePrefix);
    namespace = Optional.empty();
    tableName = Optional.empty();
  }

  @VisibleForTesting
  JdbcTransactionManager(BasicDataSource dataSource, RdbEngine rdbEngine, JdbcService jdbcService) {
    this.dataSource = dataSource;
    this.rdbEngine = rdbEngine;
    this.jdbcService = jdbcService;
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
  public JdbcTransaction start() throws TransactionException {
    String txId = UUID.randomUUID().toString();
    return start(txId);
  }

  @Override
  public JdbcTransaction start(String txId) throws TransactionException {
    try {
      return new JdbcTransaction(
          txId, jdbcService, dataSource.getConnection(), rdbEngine, namespace, tableName);
    } catch (SQLException e) {
      throw new TransactionException("failed to start the transaction", e);
    }
  }

  @Deprecated
  @Override
  public JdbcTransaction start(Isolation isolation) throws TransactionException {
    return start();
  }

  @Deprecated
  @Override
  public JdbcTransaction start(String txId, Isolation isolation) throws TransactionException {
    return start(txId);
  }

  @Deprecated
  @Override
  public JdbcTransaction start(Isolation isolation, SerializableStrategy strategy)
      throws TransactionException {
    return start();
  }

  @Deprecated
  @Override
  public JdbcTransaction start(SerializableStrategy strategy) throws TransactionException {
    return start();
  }

  @Deprecated
  @Override
  public JdbcTransaction start(String txId, SerializableStrategy strategy)
      throws TransactionException {
    return start(txId);
  }

  @Deprecated
  @Override
  public JdbcTransaction start(String txId, Isolation isolation, SerializableStrategy strategy)
      throws TransactionException {
    return start(txId);
  }

  @Override
  public TransactionState getState(String txId) {
    throw new UnsupportedOperationException("this method is not supported in JDBC transaction");
  }

  @Override
  public TransactionState abort(String txId) {
    throw new UnsupportedOperationException("this method is not supported in JDBC transaction");
  }

  @Override
  public void close() {
    try {
      dataSource.close();
    } catch (SQLException e) {
      LOGGER.warn("failed to close the dataSource", e);
    }
  }
}
