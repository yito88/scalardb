package com.scalar.db.storage.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.api.Scan;
import com.scalar.db.storage.MetadataIntegrationTestBase;
import com.scalar.db.storage.common.metadata.DataType;
import com.scalar.db.storage.jdbc.metadata.JdbcTableMetadata;
import com.scalar.db.storage.jdbc.metadata.JdbcTableMetadataManager;
import com.scalar.db.storage.jdbc.test.TestEnv;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcMetadataIntegrationTest extends MetadataIntegrationTestBase {

  private static TestEnv testEnv;
  private static JdbcTableMetadata tableMetadata;

  @Before
  public void setUp() throws Exception {
    setUp(tableMetadata);
  }

  @Test
  public void testSchemaAndTableName() {
    Optional<String> namespacePrefix = testEnv.getJdbcDatabaseConfig().getNamespacePrefix();
    String schema = namespacePrefix.orElse("") + NAMESPACE;
    assertThat(tableMetadata.getSchema()).isEqualTo(schema);
    assertThat(tableMetadata.getTable()).isEqualTo(TABLE);
    String fullTableName = schema + "." + TABLE;
    assertThat(tableMetadata.getFullTableName()).isEqualTo(fullTableName);
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    testEnv = new TestEnv();
    testEnv.register(
        NAMESPACE,
        TABLE,
        Arrays.asList(COL_NAME2, COL_NAME1),
        Arrays.asList(COL_NAME4, COL_NAME3),
        new HashMap<String, Scan.Ordering.Order>() {
          {
            put(COL_NAME4, Scan.Ordering.Order.ASC);
            put(COL_NAME3, Scan.Ordering.Order.DESC);
          }
        },
        new HashMap<String, DataType>() {
          {
            put(COL_NAME1, DataType.INT);
            put(COL_NAME2, DataType.TEXT);
            put(COL_NAME3, DataType.TEXT);
            put(COL_NAME4, DataType.INT);
            put(COL_NAME5, DataType.INT);
            put(COL_NAME6, DataType.TEXT);
            put(COL_NAME7, DataType.BIGINT);
            put(COL_NAME8, DataType.FLOAT);
            put(COL_NAME9, DataType.DOUBLE);
            put(COL_NAME10, DataType.BOOLEAN);
            put(COL_NAME11, DataType.BLOB);
          }
        },
        Arrays.asList(COL_NAME5, COL_NAME6));
    testEnv.createMetadataTable();
    testEnv.insertMetadata();

    Optional<String> namespacePrefix = testEnv.getJdbcDatabaseConfig().getNamespacePrefix();
    JdbcTableMetadataManager tableMetadataManager =
        new JdbcTableMetadataManager(testEnv.getDataSource(), namespacePrefix, testEnv.getRdbEngine());
    String fullTableName = namespacePrefix.orElse("") + NAMESPACE + "." + TABLE;
    tableMetadata = tableMetadataManager.getTableMetadata(fullTableName);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    testEnv.dropMetadataTable();
    testEnv.close();
  }
}
