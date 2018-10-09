package com.scalar.database.transaction.consensuscommit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.database.api.Consistency;
import com.scalar.database.api.DistributedStorage;
import com.scalar.database.api.Get;
import com.scalar.database.api.Put;
import com.scalar.database.api.PutIfNotExists;
import com.scalar.database.api.Result;
import com.scalar.database.api.TransactionState;
import com.scalar.database.exception.storage.ExecutionException;
import com.scalar.database.exception.transaction.CoordinatorException;
import com.scalar.database.io.BigIntValue;
import com.scalar.database.io.IntValue;
import com.scalar.database.io.TextValue;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** */
public class CoordinatorTest {
  private static final String ANY_ID_1 = "anyid1";
  private static final long ANY_TIME_1 = 1;

  @Mock private DistributedStorage storage;
  @InjectMocks private Coordinator coordinator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getState_TransactionIdGiven_ShouldReturnState()
      throws ExecutionException, CoordinatorException {
    // Arrange
    Result result = mock(Result.class);
    when(result.getValue(Attribute.ID))
        .thenReturn(Optional.of(new TextValue(Attribute.ID, ANY_ID_1)));
    when(result.getValue(Attribute.STATE))
        .thenReturn(Optional.of(new IntValue(Attribute.STATE, TransactionState.COMMITTED.get())));
    when(result.getValue(Attribute.CREATED_AT))
        .thenReturn(Optional.of(new BigIntValue(Attribute.CREATED_AT, ANY_TIME_1)));
    when(storage.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    Optional<Coordinator.State> state = coordinator.getState(ANY_ID_1);

    // Assert
    assertThat(state.get().getId()).isEqualTo(ANY_ID_1);
    Assertions.assertThat(state.get().getState()).isEqualTo(TransactionState.COMMITTED);
    assertThat(state.get().getCreatedAt()).isEqualTo(ANY_TIME_1);
  }

  @Test
  public void getState_TransactionIdGivenAndExceptionThrownInGet_ShouldThrowCoordinatorException()
      throws ExecutionException {
    // Arrange
    String id = ANY_ID_1;
    ExecutionException toThrow = mock(ExecutionException.class);
    when(storage.get(any(Get.class))).thenThrow(toThrow);

    // Act Assert
    assertThatThrownBy(
            () -> {
              coordinator.getState(id);
            })
        .isInstanceOf(CoordinatorException.class);
  }

  @Test
  public void putState_StateGiven_ShouldPutWithCorrectValues()
      throws ExecutionException, CoordinatorException {
    // Arrange
    coordinator = spy(new Coordinator(storage));
    long current = System.currentTimeMillis();
    Coordinator.State state = new Coordinator.State(ANY_ID_1, TransactionState.COMMITTED, current);
    doNothing().when(storage).put(any(Put.class));

    // Act
    coordinator.putState(state);

    // Assert
    verify(coordinator).createPutWith(state);
  }

  @Test
  public void createPutWith_StateGiven_ShouldCreateWithCorrectValues() throws ExecutionException {
    // Arrange
    long current = System.currentTimeMillis();
    Coordinator.State state = new Coordinator.State(ANY_ID_1, TransactionState.COMMITTED, current);
    doNothing().when(storage).put(any(Put.class));

    // Act
    Put put = coordinator.createPutWith(state);

    // Assert
    assertThat(put.getPartitionKey().get().get(0)).isEqualTo(new TextValue(Attribute.ID, ANY_ID_1));
    assertThat(put.getValues().get(Attribute.STATE))
        .isEqualTo(Attribute.toStateValue(TransactionState.COMMITTED));
    assertThat(put.getValues().get(Attribute.CREATED_AT))
        .isEqualTo(Attribute.toCreatedAtValue(current));
    assertThat(put.getConsistency()).isEqualTo(Consistency.LINEARIZABLE);
    assertThat(put.getCondition().get()).isExactlyInstanceOf(PutIfNotExists.class);
    assertThat(put.forNamespace().get()).isEqualTo(coordinator.NAMESPACE);
    assertThat(put.forTable().get()).isEqualTo(coordinator.TABLE);
  }

  @Test
  public void putState_StateGivenAndExceptionThrownInPut_ShouldThrowCoordinatorException()
      throws ExecutionException {
    // Arrange
    coordinator = spy(new Coordinator(storage));
    long current = System.currentTimeMillis();
    Coordinator.State state = new Coordinator.State(ANY_ID_1, TransactionState.COMMITTED, current);
    ExecutionException toThrow = mock(ExecutionException.class);
    doThrow(toThrow).when(storage).put(any(Put.class));

    // Act
    assertThatThrownBy(
            () -> {
              coordinator.putState(state);
            })
        .isInstanceOf(CoordinatorException.class);

    // Assert
    verify(coordinator).createPutWith(state);
  }
}
