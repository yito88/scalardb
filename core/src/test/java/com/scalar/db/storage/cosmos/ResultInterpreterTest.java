package com.scalar.db.storage.cosmos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.Result;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.io.DataType;
import com.scalar.db.io.Value;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ResultInterpreterTest {

  private static final String ANY_NAME_1 = "name1";
  private static final String ANY_NAME_2 = "name2";
  private static final String ANY_TEXT_1 = "text1";
  private static final String ANY_TEXT_2 = "text2";
  private static final String ANY_COLUMN_NAME_1 = "col1";
  private static final String ANY_COLUMN_NAME_2 = "col2";
  private static final String ANY_COLUMN_NAME_3 = "col3";
  private static final String ANY_COLUMN_NAME_4 = "col4";
  private static final String ANY_COLUMN_NAME_5 = "col5";
  private static final String ANY_COLUMN_NAME_6 = "col6";
  private static final String ANY_COLUMN_NAME_7 = "col7";
  private static final String ANY_ID_1 = "id";

  @Test
  public void interpret_ShouldReturnWhatsSet() {
    // Arrange
    TableMetadata metadata =
        TableMetadata.newBuilder()
            .addColumn(ANY_NAME_1, DataType.TEXT)
            .addColumn(ANY_NAME_2, DataType.TEXT)
            .addColumn(ANY_COLUMN_NAME_1, DataType.BOOLEAN)
            .addColumn(ANY_COLUMN_NAME_2, DataType.INT)
            .addColumn(ANY_COLUMN_NAME_3, DataType.BIGINT)
            .addColumn(ANY_COLUMN_NAME_4, DataType.FLOAT)
            .addColumn(ANY_COLUMN_NAME_5, DataType.DOUBLE)
            .addColumn(ANY_COLUMN_NAME_6, DataType.TEXT)
            .addColumn(ANY_COLUMN_NAME_7, DataType.BLOB)
            .addPartitionKey(ANY_NAME_1)
            .addClusteringKey(ANY_NAME_2)
            .build();

    Record record = new Record();
    record.setId(ANY_ID_1);
    record.setConcatenatedPartitionKey(ANY_TEXT_1);
    record.setPartitionKey(ImmutableMap.of(ANY_NAME_1, ANY_TEXT_1));
    record.setClusteringKey(ImmutableMap.of(ANY_NAME_2, ANY_TEXT_2));
    record.setValues(
        new HashMap<String, Object>() {
          {
            put(ANY_COLUMN_NAME_1, true);
            put(ANY_COLUMN_NAME_2, Integer.MAX_VALUE);
            put(ANY_COLUMN_NAME_3, Long.MAX_VALUE);
            put(ANY_COLUMN_NAME_4, Float.MAX_VALUE);
            put(ANY_COLUMN_NAME_5, Double.MAX_VALUE);
            put(ANY_COLUMN_NAME_6, "string");
            put(
                ANY_COLUMN_NAME_7,
                Base64.getEncoder().encodeToString("bytes".getBytes(StandardCharsets.UTF_8)));
          }
        });

    List<String> projections = Collections.emptyList();
    ResultInterpreter spy = spy(new ResultInterpreter(projections, metadata));

    // Act
    Result result = spy.interpret(record);

    // Assert
    assertThat(result.getValue(ANY_NAME_1).isPresent()).isTrue();
    assertThat(result.getValue(ANY_NAME_1).get().getAsString().isPresent()).isTrue();
    assertThat(result.getValue(ANY_NAME_1).get().getAsString().get()).isEqualTo(ANY_TEXT_1);
    assertThat(result.getValue(ANY_NAME_2).isPresent()).isTrue();
    assertThat(result.getValue(ANY_NAME_2).get().getAsString().isPresent()).isTrue();
    assertThat(result.getValue(ANY_NAME_2).get().getAsString().get()).isEqualTo(ANY_TEXT_2);
    assertThat(result.getValue(ANY_COLUMN_NAME_1).isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_1).get().getAsBoolean()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_2).isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_2).get().getAsInt()).isEqualTo(Integer.MAX_VALUE);
    assertThat(result.getValue(ANY_COLUMN_NAME_3).isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_3).get().getAsLong()).isEqualTo(Long.MAX_VALUE);
    assertThat(result.getValue(ANY_COLUMN_NAME_4).isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_4).get().getAsFloat()).isEqualTo(Float.MAX_VALUE);
    assertThat(result.getValue(ANY_COLUMN_NAME_5).isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_5).get().getAsDouble()).isEqualTo(Double.MAX_VALUE);
    assertThat(result.getValue(ANY_COLUMN_NAME_6).isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_6).get().getAsString().isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_6).get().getAsString().get()).isEqualTo("string");
    assertThat(result.getValue(ANY_COLUMN_NAME_7).isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_7).get().getAsBytes().isPresent()).isTrue();
    assertThat(result.getValue(ANY_COLUMN_NAME_7).get().getAsBytes().get())
        .isEqualTo("bytes".getBytes());

    Map<String, Value<?>> values = result.getValues();
    assertThat(values.containsKey(ANY_NAME_1)).isTrue();
    assertThat(values.get(ANY_NAME_1).getAsString().isPresent()).isTrue();
    assertThat(values.get(ANY_NAME_1).getAsString().get()).isEqualTo(ANY_TEXT_1);
    assertThat(values.containsKey(ANY_NAME_2)).isTrue();
    assertThat(values.get(ANY_NAME_2).getAsString().isPresent()).isTrue();
    assertThat(values.get(ANY_NAME_2).getAsString().get()).isEqualTo(ANY_TEXT_2);
    assertThat(values.containsKey(ANY_COLUMN_NAME_1)).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_1).getAsBoolean()).isTrue();
    assertThat(values.containsKey(ANY_COLUMN_NAME_2)).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_2).getAsInt()).isEqualTo(Integer.MAX_VALUE);
    assertThat(values.containsKey(ANY_COLUMN_NAME_3)).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_3).getAsLong()).isEqualTo(Long.MAX_VALUE);
    assertThat(values.containsKey(ANY_COLUMN_NAME_4)).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_4).getAsFloat()).isEqualTo(Float.MAX_VALUE);
    assertThat(values.containsKey(ANY_COLUMN_NAME_5)).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_5).getAsDouble()).isEqualTo(Double.MAX_VALUE);
    assertThat(values.containsKey(ANY_COLUMN_NAME_6)).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_6).getAsString().isPresent()).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_6).getAsString().get()).isEqualTo("string");
    assertThat(values.containsKey(ANY_COLUMN_NAME_7)).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_7).getAsBytes().isPresent()).isTrue();
    assertThat(values.get(ANY_COLUMN_NAME_7).getAsBytes().get()).isEqualTo("bytes".getBytes());
  }
}
