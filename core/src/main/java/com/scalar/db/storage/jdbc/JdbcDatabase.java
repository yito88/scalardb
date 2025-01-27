package com.scalar.db.storage.jdbc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Mutation;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scanner;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.storage.common.checker.OperationChecker;
import com.scalar.db.storage.jdbc.query.QueryBuilder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A storage implementation with JDBC for {@link DistributedStorage}.
 *
 * <p>Note that the consistency in an operation is always LINEARIZABLE in this implementation. Even
 * if consistency is specified in an operation, it will be ignored.
 *
 * @author Toshihiro Suzuki
 */
@ThreadSafe
public class JdbcDatabase implements DistributedStorage {
  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDatabase.class);

  private final BasicDataSource dataSource;
  private final JdbcService jdbcService;
  private Optional<String> namespace;
  private Optional<String> tableName;

  @Inject
  public JdbcDatabase(JdbcConfig config) {
    dataSource = JdbcUtils.initDataSource(config);
    Optional<String> namespacePrefix = config.getNamespacePrefix();
    RdbEngine rdbEngine = JdbcUtils.getRdbEngine(config.getContactPoints().get(0));
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
  JdbcDatabase(BasicDataSource dataSource, JdbcService jdbcService) {
    this.dataSource = dataSource;
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
  public Optional<Result> get(Get get) throws ExecutionException {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      return jdbcService.get(get, connection, namespace, tableName);
    } catch (SQLException e) {
      throw new ExecutionException("get operation failed", e);
    } finally {
      close(connection);
    }
  }

  @Override
  public Scanner scan(Scan scan) throws ExecutionException {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      return jdbcService.getScanner(scan, connection, namespace, tableName);
    } catch (SQLException e) {
      close(connection);
      throw new ExecutionException("scan operation failed", e);
    }
  }

  @Override
  public void put(Put put) throws ExecutionException {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      if (!jdbcService.put(put, connection, namespace, tableName)) {
        throw new NoMutationException("no mutation was applied");
      }
    } catch (SQLException e) {
      throw new ExecutionException("put operation failed", e);
    } finally {
      close(connection);
    }
  }

  @Override
  public void put(List<Put> puts) throws ExecutionException {
    mutate(puts);
  }

  @Override
  public void delete(Delete delete) throws ExecutionException {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      if (!jdbcService.delete(delete, connection, namespace, tableName)) {
        throw new NoMutationException("no mutation was applied");
      }
    } catch (SQLException e) {
      throw new ExecutionException("delete operation failed", e);
    } finally {
      close(connection);
    }
  }

  @Override
  public void delete(List<Delete> deletes) throws ExecutionException {
    mutate(deletes);
  }

  @Override
  public void mutate(List<? extends Mutation> mutations) throws ExecutionException {
    if (mutations.size() == 1) {
      Mutation mutation = mutations.get(0);
      if (mutation instanceof Put) {
        put((Put) mutation);
      } else if (mutation instanceof Delete) {
        delete((Delete) mutation);
      }
      return;
    }

    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      connection.setAutoCommit(false);
    } catch (SQLException e) {
      close(connection);
      throw new ExecutionException("mutate operation failed", e);
    }

    try {
      if (!jdbcService.mutate(mutations, connection, namespace, tableName)) {
        try {
          connection.rollback();
        } catch (SQLException e) {
          throw new ExecutionException("failed to rollback", e);
        }
        throw new NoMutationException("no mutation was applied");
      } else {
        connection.commit();
      }
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException sqlException) {
        throw new ExecutionException("failed to rollback", sqlException);
      }
      throw new ExecutionException("mutate operation failed", e);
    } finally {
      close(connection);
    }
  }

  private void close(Connection connection) {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      LOGGER.warn("failed to close the connection", e);
    }
  }

  @Override
  public void close() {
    try {
      dataSource.close();
    } catch (SQLException e) {
      LOGGER.error("failed to close the dataSource", e);
    }
  }
}
