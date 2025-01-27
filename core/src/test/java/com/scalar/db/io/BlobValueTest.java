package com.scalar.db.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;

/** */
public class BlobValueTest {
  private static final String ANY_NAME = "name";
  private static final String ANOTHER_NAME = "another_name";

  @Test
  public void get_ProperValueGivenInConstructor_ShouldReturnWhatsSet() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    BlobValue value = new BlobValue(ANY_NAME, expected);

    // Act
    Optional<byte[]> actual = value.get();

    // Assert
    assertThat(actual.isPresent()).isTrue();
    assertThat(Arrays.equals(expected, actual.get())).isTrue();
    assertThat(expected == actual.get()).isFalse();
  }

  @Test
  public void getAsBytes_ProperValueGivenInConstructor_ShouldReturnWhatsSet() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    Value<?> value = new BlobValue(ANY_NAME, expected);

    // Act
    Optional<byte[]> actual = value.getAsBytes();

    // Assert
    assertThat(actual.isPresent()).isTrue();
    assertThat(Arrays.equals(expected, actual.get())).isTrue();
    assertThat(expected == actual.get()).isFalse();
  }

  @Test
  public void
      getAsBoolean_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    Value<?> value = new BlobValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsBoolean).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getAsInt_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    Value<?> value = new BlobValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsInt).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getAsLong_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    Value<?> value = new BlobValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsLong).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getAsFloat_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    Value<?> value = new BlobValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsFloat).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getAsDouble_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    Value<?> value = new BlobValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsDouble).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getAsString_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    byte[] expected = "some_text".getBytes();
    Value<?> value = new BlobValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsString).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void copyWith_WithValuePresent_ShouldReturnNewBlobWithSameValue() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_text".getBytes());

    // Act
    BlobValue newValue = oneValue.copyWith("new name");

    // Assert
    assertThat(oneValue.get().get()).isEqualTo(newValue.get().get());
  }

  @Test
  public void copyWith_WithValueEmpty_ShouldReturnNewBlobWithValueEmpty() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, null);

    // Act
    BlobValue newValue = oneValue.copyWith("new name");

    // Assert
    assertThat(newValue.get()).isEmpty();
  }

  @Test
  public void equals_DifferentObjectsSameValuesGiven_ShouldReturnTrue() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_text".getBytes());
    BlobValue anotherValue = new BlobValue(ANY_NAME, "some_text".getBytes());

    // Act
    boolean result = oneValue.equals(anotherValue);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_DifferentObjectsSameValuesDifferentNamesGiven_ShouldReturnFalse() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_text".getBytes());
    BlobValue anotherValue = new BlobValue(ANOTHER_NAME, "some_text".getBytes());

    // Act
    boolean result = oneValue.equals(anotherValue);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_SameObjectsGiven_ShouldReturnTrue() {
    // Arrange
    BlobValue value = new BlobValue(ANY_NAME, "some_text".getBytes());

    // Act
    boolean result = value.equals(value);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_DifferentObjectsDifferentValuesGiven_ShouldReturnFalse() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_text".getBytes());
    BlobValue anotherValue = new BlobValue(ANY_NAME, "another_text".getBytes());

    // Act
    boolean result = oneValue.equals(anotherValue);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_DifferentTypesSameValuesGiven_ShouldReturnFalse() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_text".getBytes());
    TextValue anotherValue = new TextValue(ANY_NAME, "some_text");

    // Act
    boolean result = oneValue.equals(anotherValue);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void compareTo_ThisBiggerThanGiven_ShouldReturnPositive() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_value2".getBytes());
    BlobValue anotherValue = new BlobValue(ANY_NAME, "some_value1".getBytes());

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual > 0).isTrue();
  }

  @Test
  public void compareTo_ThisEqualsToGiven_ShouldReturnZero() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_value".getBytes());
    BlobValue anotherValue = new BlobValue(ANY_NAME, "some_value".getBytes());

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual == 0).isTrue();
  }

  @Test
  public void compareTo_ThisSmallerThanGiven_ShouldReturnNegative() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_value1".getBytes());
    BlobValue anotherValue = new BlobValue(ANY_NAME, "some_value2".getBytes());

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual < 0).isTrue();
  }

  @Test
  public void compareTo_ThisNonNullAndGivenNull_ShouldReturnPositive() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, "some_value".getBytes());
    BlobValue anotherValue = new BlobValue(ANY_NAME, null);

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual > 0).isTrue();
  }

  @Test
  public void compareTo_ThisNullAndGivenNonNull_ShouldReturnNegative() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, null);
    BlobValue anotherValue = new BlobValue(ANY_NAME, "some_value".getBytes());

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual < 0).isTrue();
  }

  @Test
  public void compareTo_ThisAndGivenAreNull_ShouldReturnZero() {
    // Arrange
    BlobValue oneValue = new BlobValue(ANY_NAME, null);
    BlobValue anotherValue = new BlobValue(ANY_NAME, null);

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual == 0).isTrue();
  }

  @Test
  public void constructor_NullGiven_ShouldThrowNullPointerException() {
    // Act Assert
    assertThatThrownBy(
            () -> {
              new BlobValue(null, null);
            })
        .isInstanceOf(NullPointerException.class);
  }
}
