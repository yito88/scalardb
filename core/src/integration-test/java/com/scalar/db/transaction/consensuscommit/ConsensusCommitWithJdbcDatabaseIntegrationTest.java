package com.scalar.db.transaction.consensuscommit;

import static com.scalar.db.transaction.consensuscommit.Attribute.BEFORE_COMMITTED_AT;
import static com.scalar.db.transaction.consensuscommit.Attribute.BEFORE_ID;
import static com.scalar.db.transaction.consensuscommit.Attribute.BEFORE_PREFIX;
import static com.scalar.db.transaction.consensuscommit.Attribute.BEFORE_PREPARED_AT;
import static com.scalar.db.transaction.consensuscommit.Attribute.BEFORE_STATE;
import static com.scalar.db.transaction.consensuscommit.Attribute.BEFORE_VERSION;
import static com.scalar.db.transaction.consensuscommit.Attribute.COMMITTED_AT;
import static com.scalar.db.transaction.consensuscommit.Attribute.CREATED_AT;
import static com.scalar.db.transaction.consensuscommit.Attribute.ID;
import static com.scalar.db.transaction.consensuscommit.Attribute.PREPARED_AT;
import static com.scalar.db.transaction.consensuscommit.Attribute.STATE;
import static com.scalar.db.transaction.consensuscommit.Attribute.VERSION;
import static org.mockito.Mockito.spy;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.io.DataType;
import com.scalar.db.storage.jdbc.JdbcDatabase;
import com.scalar.db.storage.jdbc.test.TestEnv;
import java.sql.SQLException;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class ConsensusCommitWithJdbcDatabaseIntegrationTest
    extends ConsensusCommitIntegrationTestBase {

  private static TestEnv testEnv;
  private static DistributedStorage originalStorage;

  @Before
  public void setUp() throws SQLException {
    DistributedStorage storage = spy(originalStorage);
    Coordinator coordinator = spy(new Coordinator(storage));
    RecoveryHandler recovery = spy(new RecoveryHandler(storage, coordinator));
    CommitHandler commit = spy(new CommitHandler(storage, coordinator, recovery));
    ConsensusCommitManager manager =
        new ConsensusCommitManager(storage, testEnv.getJdbcConfig(), coordinator, recovery, commit);
    setUp(manager, storage, coordinator, recovery);
  }

  @After
  public void tearDown() throws Exception {
    testEnv.deleteTableData();
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    testEnv = new TestEnv();

    // For the coordinator table
    testEnv.register(
        Coordinator.NAMESPACE,
        Coordinator.TABLE,
        TableMetadata.newBuilder()
            .addColumn(ID, DataType.TEXT)
            .addColumn(STATE, DataType.INT)
            .addColumn(CREATED_AT, DataType.BIGINT)
            .addPartitionKey(ID)
            .build());

    // For the test tables
    for (String table : Arrays.asList(TABLE_1, TABLE_2)) {
      testEnv.register(
          NAMESPACE,
          table,
          TableMetadata.newBuilder()
              .addColumn(ACCOUNT_ID, DataType.INT)
              .addColumn(ACCOUNT_TYPE, DataType.INT)
              .addColumn(BALANCE, DataType.INT)
              .addColumn(ID, DataType.TEXT)
              .addColumn(STATE, DataType.INT)
              .addColumn(VERSION, DataType.INT)
              .addColumn(PREPARED_AT, DataType.BIGINT)
              .addColumn(COMMITTED_AT, DataType.BIGINT)
              .addColumn(BEFORE_PREFIX + BALANCE, DataType.INT)
              .addColumn(BEFORE_ID, DataType.TEXT)
              .addColumn(BEFORE_STATE, DataType.INT)
              .addColumn(BEFORE_VERSION, DataType.INT)
              .addColumn(BEFORE_PREPARED_AT, DataType.BIGINT)
              .addColumn(BEFORE_COMMITTED_AT, DataType.BIGINT)
              .addPartitionKey(ACCOUNT_ID)
              .addClusteringKey(ACCOUNT_TYPE)
              .build());
    }

    testEnv.createMetadataTable();
    testEnv.createTables();
    testEnv.insertMetadata();

    originalStorage = new JdbcDatabase(testEnv.getJdbcConfig());
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    testEnv.dropMetadataTable();
    testEnv.dropTables();
    testEnv.close();
  }
}
