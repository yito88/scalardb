package com.scalar.db.storage.dynamo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Get;
import com.scalar.db.api.Operation;
import com.scalar.db.api.Scan;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

public class SelectStatementHandlerTest {
  private static final String ANY_KEYSPACE_NAME = "keyspace";
  private static final String ANY_TABLE_NAME = "table";
  private static final String ANY_NAME_1 = "name1";
  private static final String ANY_NAME_2 = "name2";
  private static final String ANY_NAME_3 = "name3";
  private static final String ANY_TEXT_1 = "text1";
  private static final String ANY_TEXT_2 = "text2";
  private static final String ANY_TEXT_3 = "text3";
  private static final String ANY_TEXT_4 = "text4";
  private static final Scan.Ordering.Order ASC_ORDER = Scan.Ordering.Order.ASC;
  private static final Scan.Ordering.Order DESC_ORDER = Scan.Ordering.Order.DESC;
  private static final int ANY_LIMIT = 100;

  private SelectStatementHandler handler;
  @Mock private DynamoDbClient client;
  @Mock private DynamoTableMetadataManager metadataManager;
  @Mock private TableMetadata metadata;
  @Mock private GetItemResponse getResponse;
  @Mock private QueryResponse queryResponse;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    handler = new SelectStatementHandler(client, metadataManager);

    when(metadataManager.getTableMetadata(any(Operation.class))).thenReturn(metadata);
    when(metadata.getPartitionKeyNames())
        .thenReturn(new LinkedHashSet<>(Collections.singletonList(ANY_NAME_1)));
    when(metadata.getClusteringKeyNames())
        .thenReturn(new LinkedHashSet<>(Collections.singletonList(ANY_NAME_2)));
    when(metadata.getSecondaryIndexNames())
        .thenReturn(new HashSet<>(Collections.singletonList(ANY_NAME_3)));
  }

  private Get prepareGet() {
    Key partitionKey = new Key(new TextValue(ANY_NAME_1, ANY_TEXT_1));
    Key clusteringKey = new Key(new TextValue(ANY_NAME_2, ANY_TEXT_2));
    return new Get(partitionKey, clusteringKey)
        .forNamespace(ANY_KEYSPACE_NAME)
        .forTable(ANY_TABLE_NAME);
  }

  private Scan prepareScan() {
    Key partitionKey = new Key(new TextValue(ANY_NAME_1, ANY_TEXT_1));
    return new Scan(partitionKey).forNamespace(ANY_KEYSPACE_NAME).forTable(ANY_TABLE_NAME);
  }

  @Test
  public void handle_GetOperationGiven_ShouldCallGetItem() {
    // Arrange
    when(client.getItem(any(GetItemRequest.class))).thenReturn(getResponse);
    when(getResponse.hasItem()).thenReturn(true);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(getResponse.item()).thenReturn(expected);
    Get get = prepareGet();
    DynamoOperation dynamoOperation = new DynamoOperation(get, metadataManager);
    Map<String, AttributeValue> expectedKeys = dynamoOperation.getKeyMap();

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(get);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
    verify(client).getItem(captor.capture());
    GetItemRequest actualRequest = captor.getValue();
    assertThat(actualRequest.key()).isEqualTo(expectedKeys);
    assertThat(actualRequest.projectionExpression()).isNull();
  }

  @Test
  public void handle_GetOperationNoItemReturned_ShouldReturnEmptyList() throws Exception {
    // Arrange
    when(client.getItem(any(GetItemRequest.class))).thenReturn(getResponse);
    when(getResponse.hasItem()).thenReturn(false);

    Get get = prepareGet();

    // Act Assert
    List<Map<String, AttributeValue>> actual = handler.handle(get);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  public void handle_GetOperationWithIndexGiven_ShouldCallQuery() {
    // Arrange
    when(client.query(any(QueryRequest.class))).thenReturn(queryResponse);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(queryResponse.items()).thenReturn(Arrays.asList(expected));

    Key indexKey = new Key(new TextValue(ANY_NAME_3, ANY_TEXT_3));
    Get get = new Get(indexKey).forNamespace(ANY_KEYSPACE_NAME).forTable(ANY_TABLE_NAME);
    String expectedKeyCondition = ANY_NAME_3 + " = " + DynamoOperation.VALUE_ALIAS + "0";
    Map<String, AttributeValue> expectedBindMap = new HashMap<>();
    expectedBindMap.put(
        DynamoOperation.VALUE_ALIAS + "0", AttributeValue.builder().s(ANY_TEXT_3).build());

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(get);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(captor.capture());
    QueryRequest actualRequest = captor.getValue();
    assertThat(actualRequest.keyConditionExpression()).isEqualTo(expectedKeyCondition);
    assertThat(actualRequest.expressionAttributeValues()).isEqualTo(expectedBindMap);
  }

  @Test
  public void handle_GetOperationDynamoDbExceptionThrown_ShouldThrowExecutionException() {
    // Arrange
    DynamoDbException toThrow = mock(DynamoDbException.class);
    doThrow(toThrow).when(client).getItem(any(GetItemRequest.class));

    Get get = prepareGet();

    // Act Assert
    assertThatThrownBy(
            () -> {
              handler.handle(get);
            })
        .isInstanceOf(ExecutionException.class)
        .hasCause(toThrow);
  }

  @Test
  public void handle_ScanOperationGiven_ShouldCallQuery() {
    // Arrange
    when(client.query(any(QueryRequest.class))).thenReturn(queryResponse);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(queryResponse.items()).thenReturn(Arrays.asList(expected));
    Scan scan = prepareScan();
    String expectedKeyCondition =
        DynamoOperation.PARTITION_KEY + " = " + DynamoOperation.PARTITION_KEY_ALIAS;
    DynamoOperation dynamoOperation = new DynamoOperation(scan, metadataManager);
    String partitionKey = dynamoOperation.getConcatenatedPartitionKey();
    Map<String, AttributeValue> expectedBindMap = new HashMap<>();
    expectedBindMap.put(
        DynamoOperation.PARTITION_KEY_ALIAS, AttributeValue.builder().s(partitionKey).build());

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(scan);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(captor.capture());
    QueryRequest actualRequest = captor.getValue();
    assertThat(actualRequest.keyConditionExpression()).isEqualTo(expectedKeyCondition);
    assertThat(actualRequest.expressionAttributeValues()).isEqualTo(expectedBindMap);
  }

  @Test
  public void handle_ScanOperationWithIndexGiven_ShouldCallQuery() {
    // Arrange
    when(client.query(any(QueryRequest.class))).thenReturn(queryResponse);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(queryResponse.items()).thenReturn(Arrays.asList(expected));

    Key indexKey = new Key(new TextValue(ANY_NAME_3, ANY_TEXT_3));
    Scan scan = new Scan(indexKey).forNamespace(ANY_KEYSPACE_NAME).forTable(ANY_TABLE_NAME);
    String expectedKeyCondition = ANY_NAME_3 + " = " + DynamoOperation.VALUE_ALIAS + "0";
    Map<String, AttributeValue> expectedBindMap = new HashMap<>();
    expectedBindMap.put(
        DynamoOperation.VALUE_ALIAS + "0", AttributeValue.builder().s(ANY_TEXT_3).build());

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(scan);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(captor.capture());
    QueryRequest actualRequest = captor.getValue();
    assertThat(actualRequest.keyConditionExpression()).isEqualTo(expectedKeyCondition);
    assertThat(actualRequest.expressionAttributeValues()).isEqualTo(expectedBindMap);
  }

  @Test
  public void handle_ScanOperationCosmosExceptionThrown_ShouldThrowExecutionException() {
    // Arrange
    DynamoDbException toThrow = mock(DynamoDbException.class);
    doThrow(toThrow).when(client).query(any(QueryRequest.class));

    Scan scan = prepareScan();

    // Act Assert
    assertThatThrownBy(
            () -> {
              handler.handle(scan);
            })
        .isInstanceOf(ExecutionException.class)
        .hasCause(toThrow);
  }

  @Test
  public void handle_ScanOperationWithSingleClusteringKey_ShouldCallQueryItemsWithProperQuery() {
    // Arrange
    when(client.query(any(QueryRequest.class))).thenReturn(queryResponse);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(queryResponse.items()).thenReturn(Arrays.asList(expected));

    Scan scan =
        prepareScan()
            .withStart(new Key(new TextValue(ANY_NAME_2, ANY_TEXT_2)))
            .withEnd(new Key(new TextValue(ANY_NAME_2, ANY_TEXT_3)));

    String expectedCondition =
        DynamoOperation.PARTITION_KEY
            + " = "
            + DynamoOperation.PARTITION_KEY_ALIAS
            + " AND "
            + ANY_NAME_2
            + DynamoOperation.RANGE_CONDITION;
    DynamoOperation dynamoOperation = new DynamoOperation(scan, metadataManager);
    String partitionKey = dynamoOperation.getConcatenatedPartitionKey();
    Map<String, AttributeValue> expectedBindMap = new HashMap<>();
    expectedBindMap.put(
        DynamoOperation.PARTITION_KEY_ALIAS, AttributeValue.builder().s(partitionKey).build());
    expectedBindMap.put(
        DynamoOperation.RANGE_KEY_ALIAS + "0", AttributeValue.builder().s(ANY_TEXT_2).build());
    expectedBindMap.put(
        DynamoOperation.RANGE_KEY_ALIAS + "1", AttributeValue.builder().s(ANY_TEXT_3).build());

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(scan);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(captor.capture());
    QueryRequest actualRequest = captor.getValue();
    assertThat(actualRequest.keyConditionExpression()).isEqualTo(expectedCondition);
    assertThat(actualRequest.expressionAttributeValues()).isEqualTo(expectedBindMap);
  }

  @Test
  public void handle_ScanOperationWithMultipleClusteringKeys_ShouldCallQueryItemsWithProperQuery() {
    // Arrange
    when(client.query(any(QueryRequest.class))).thenReturn(queryResponse);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(queryResponse.items()).thenReturn(Arrays.asList(expected));

    Scan scan =
        prepareScan()
            .withStart(
                new Key(
                    new TextValue(ANY_NAME_2, ANY_TEXT_2), new TextValue(ANY_NAME_3, ANY_TEXT_3)))
            .withEnd(
                new Key(
                    new TextValue(ANY_NAME_2, ANY_TEXT_2), new TextValue(ANY_NAME_3, ANY_TEXT_4)));

    String expectedCondition =
        DynamoOperation.PARTITION_KEY
            + " = "
            + DynamoOperation.PARTITION_KEY_ALIAS
            + " AND "
            + ANY_NAME_3
            + DynamoOperation.RANGE_CONDITION;
    String expectedFilter =
        ANY_NAME_2
            + " = "
            + DynamoOperation.START_CLUSTERING_KEY_ALIAS
            + "0 AND "
            + ANY_NAME_2
            + " = "
            + DynamoOperation.END_CLUSTERING_KEY_ALIAS
            + "0";
    DynamoOperation dynamoOperation = new DynamoOperation(scan, metadataManager);
    String partitionKey = dynamoOperation.getConcatenatedPartitionKey();
    Map<String, AttributeValue> expectedBindMap = new HashMap<>();
    expectedBindMap.put(
        DynamoOperation.PARTITION_KEY_ALIAS, AttributeValue.builder().s(partitionKey).build());
    expectedBindMap.put(
        DynamoOperation.RANGE_KEY_ALIAS + "0", AttributeValue.builder().s(ANY_TEXT_3).build());
    expectedBindMap.put(
        DynamoOperation.RANGE_KEY_ALIAS + "1", AttributeValue.builder().s(ANY_TEXT_4).build());
    expectedBindMap.put(
        DynamoOperation.START_CLUSTERING_KEY_ALIAS + "0",
        AttributeValue.builder().s(ANY_TEXT_2).build());
    expectedBindMap.put(
        DynamoOperation.END_CLUSTERING_KEY_ALIAS + "0",
        AttributeValue.builder().s(ANY_TEXT_2).build());

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(scan);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(captor.capture());
    QueryRequest actualRequest = captor.getValue();
    assertThat(actualRequest.keyConditionExpression()).isEqualTo(expectedCondition);
    assertThat(actualRequest.filterExpression()).isEqualTo(expectedFilter);
    assertThat(actualRequest.expressionAttributeValues()).isEqualTo(expectedBindMap);
  }

  @Test
  public void handle_ScanOperationWithOrderingAndLimit_ShouldCallQueryWithProperRequest() {
    // Arrange
    when(client.query(any(QueryRequest.class))).thenReturn(queryResponse);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(queryResponse.items()).thenReturn(Arrays.asList(expected));

    Scan scan =
        prepareScan()
            .withStart(new Key(new TextValue(ANY_NAME_2, ANY_TEXT_2)))
            .withOrdering(new Scan.Ordering(ANY_NAME_2, ASC_ORDER))
            .withLimit(ANY_LIMIT);

    String expectedCondition =
        DynamoOperation.PARTITION_KEY
            + " = "
            + DynamoOperation.PARTITION_KEY_ALIAS
            + " AND "
            + ANY_NAME_2
            + " >= "
            + DynamoOperation.START_CLUSTERING_KEY_ALIAS
            + "0";
    DynamoOperation dynamoOperation = new DynamoOperation(scan, metadataManager);
    String partitionKey = dynamoOperation.getConcatenatedPartitionKey();
    Map<String, AttributeValue> expectedBindMap = new HashMap<>();
    expectedBindMap.put(
        DynamoOperation.PARTITION_KEY_ALIAS, AttributeValue.builder().s(partitionKey).build());
    expectedBindMap.put(
        DynamoOperation.START_CLUSTERING_KEY_ALIAS + "0",
        AttributeValue.builder().s(ANY_TEXT_2).build());

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(scan);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(captor.capture());
    QueryRequest actualRequest = captor.getValue();
    assertThat(actualRequest.keyConditionExpression()).isEqualTo(expectedCondition);
    assertThat(actualRequest.expressionAttributeValues()).isEqualTo(expectedBindMap);
    assertThat(actualRequest.scanIndexForward()).isNull();
    assertThat(actualRequest.limit()).isEqualTo(ANY_LIMIT);
  }

  @Test
  public void handle_ScanOperationWithMultipleOrdering_ShouldCallQueryWithProperRequest() {
    // Arrange
    when(client.query(any(QueryRequest.class))).thenReturn(queryResponse);
    Map<String, AttributeValue> expected = new HashMap<>();
    when(queryResponse.items()).thenReturn(Arrays.asList(expected));

    Scan scan =
        prepareScan()
            .withStart(new Key(new TextValue(ANY_NAME_2, ANY_TEXT_2)))
            .withOrdering(new Scan.Ordering(ANY_NAME_2, ASC_ORDER))
            .withOrdering(new Scan.Ordering(ANY_NAME_3, DESC_ORDER))
            .withLimit(ANY_LIMIT);

    String expectedCondition =
        DynamoOperation.PARTITION_KEY
            + " = "
            + DynamoOperation.PARTITION_KEY_ALIAS
            + " AND "
            + ANY_NAME_2
            + " >= "
            + DynamoOperation.START_CLUSTERING_KEY_ALIAS
            + "0";
    DynamoOperation dynamoOperation = new DynamoOperation(scan, metadataManager);
    String partitionKey = dynamoOperation.getConcatenatedPartitionKey();
    Map<String, AttributeValue> expectedBindMap = new HashMap<>();
    expectedBindMap.put(
        DynamoOperation.PARTITION_KEY_ALIAS, AttributeValue.builder().s(partitionKey).build());
    expectedBindMap.put(
        DynamoOperation.START_CLUSTERING_KEY_ALIAS + "0",
        AttributeValue.builder().s(ANY_TEXT_2).build());

    // Act Assert
    assertThatCode(
            () -> {
              handler.handle(scan);
            })
        .doesNotThrowAnyException();

    // Assert
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(captor.capture());
    QueryRequest actualRequest = captor.getValue();
    assertThat(actualRequest.keyConditionExpression()).isEqualTo(expectedCondition);
    assertThat(actualRequest.expressionAttributeValues()).isEqualTo(expectedBindMap);
    assertThat(actualRequest.scanIndexForward()).isNull();
    assertThat(actualRequest.limit()).isEqualTo(ANY_LIMIT);
  }
}
