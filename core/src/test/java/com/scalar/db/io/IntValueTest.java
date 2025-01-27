package com.scalar.db.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

/** */
public class IntValueTest {
  private static final String ANY_NAME = "name";
  private static final String ANOTHER_NAME = "another_name";

  @Test
  public void get_ProperValueGivenInConstructor_ShouldReturnWhatsSet() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    IntValue value = new IntValue(ANY_NAME, expected);

    // Act
    int actual = value.get();

    // Assert
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  public void getAsInt_ProperValueGivenInConstructor_ShouldReturnWhatsSet() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    Value<?> value = new IntValue(ANY_NAME, expected);

    // Act
    int actual = value.getAsInt();

    // Assert
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  public void getAsLong_ProperValueGivenInConstructor_ShouldReturnWhatsSet() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    Value<?> value = new IntValue(ANY_NAME, expected);

    // Act
    long actual = value.getAsLong();

    // Assert
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  public void getAsFloat_ProperValueGivenInConstructor_ShouldReturnWhatsSet() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    Value<?> value = new IntValue(ANY_NAME, expected);

    // Act
    float actual = value.getAsFloat();

    // Assert
    assertThat((float) expected).isEqualTo(actual);
  }

  @Test
  public void getAsDouble_ProperValueGivenInConstructor_ShouldReturnWhatsSet() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    Value<?> value = new IntValue(ANY_NAME, expected);

    // Act
    double actual = value.getAsDouble();

    // Assert
    assertThat((double) expected).isEqualTo(actual);
  }

  @Test
  public void
      getAsBoolean_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    Value<?> value = new IntValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsBoolean).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getAsString_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    Value<?> value = new IntValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsString).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void getAsBytes_ProperValueGivenInConstructor_ShouldThrowUnsupportedOperationException() {
    // Arrange
    int expected = Integer.MAX_VALUE;
    Value<?> value = new IntValue(ANY_NAME, expected);

    // Act Assert
    assertThatThrownBy(value::getAsBytes).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void equals_DifferentObjectsSameValuesGiven_ShouldReturnTrue() {
    // Arrange
    int some = Integer.MAX_VALUE;
    IntValue one = new IntValue(ANY_NAME, some);
    IntValue another = new IntValue(ANY_NAME, some);

    // Act
    boolean result = one.equals(another);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_DifferentObjectsSameValuesDifferentNamesGiven_ShouldReturnFalse() {
    // Arrange
    int some = Integer.MAX_VALUE;
    IntValue one = new IntValue(ANY_NAME, some);
    IntValue another = new IntValue(ANOTHER_NAME, some);

    // Act
    boolean result = one.equals(another);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_SameObjectsGiven_ShouldReturnTrue() {
    // Arrange
    int some = Integer.MAX_VALUE;
    IntValue value = new IntValue(ANY_NAME, some);

    // Act
    boolean result = value.equals(value);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_DifferentObjectsDifferentValuesGiven_ShouldReturnFalse() {
    // Arrange
    int one = Integer.MAX_VALUE;
    int another = Integer.MAX_VALUE - 1;
    IntValue oneValue = new IntValue(ANY_NAME, one);
    IntValue anotherValue = new IntValue(ANY_NAME, another);

    // Act
    boolean result = oneValue.equals(anotherValue);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_DifferentTypesSameValuesGiven_ShouldReturnFalse() {
    // Arrange
    int some = Integer.MAX_VALUE;
    IntValue one = new IntValue(ANY_NAME, some);
    BigIntValue another = new BigIntValue(ANY_NAME, some);

    // Act
    boolean result = one.equals(another);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void compareTo_ThisBiggerThanGiven_ShouldReturnPositive() {
    // Arrange
    int one = Integer.MAX_VALUE;
    int another = Integer.MAX_VALUE - 1;
    IntValue oneValue = new IntValue(ANY_NAME, one);
    IntValue anotherValue = new IntValue(ANY_NAME, another);

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual > 0).isTrue();
  }

  @Test
  public void compareTo_ThisEqualsToGiven_ShouldReturnZero() {
    // Arrange
    int one = Integer.MAX_VALUE;
    int another = Integer.MAX_VALUE;
    IntValue oneValue = new IntValue(ANY_NAME, one);
    IntValue anotherValue = new IntValue(ANY_NAME, another);

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual == 0).isTrue();
  }

  @Test
  public void compareTo_ThisSmallerThanGiven_ShouldReturnNegative() {
    // Arrange
    int one = Integer.MAX_VALUE - 1;
    int another = Integer.MAX_VALUE;
    IntValue oneValue = new IntValue(ANY_NAME, one);
    IntValue anotherValue = new IntValue(ANY_NAME, another);

    // Act
    int actual = oneValue.compareTo(anotherValue);

    // Assert
    assertThat(actual < 0).isTrue();
  }

  @Test
  public void constructor_NullGiven_ShouldThrowNullPointerException() {
    // Act Assert
    assertThatThrownBy(
            () -> {
              new IntValue(null, 1);
            })
        .isInstanceOf(NullPointerException.class);
  }
}
