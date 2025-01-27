package com.scalar.db.io;

import java.util.Optional;

/**
 * An abstraction for storage entry's value
 *
 * @author Hiroyuki Yamada
 */
public interface Value<T> extends Comparable<T> {

  /**
   * Returns the name of the value
   *
   * @return the name of this value
   */
  String getName();

  /**
   * Creates a copy of the value with the specified name
   *
   * @param name name of a {@code Value}
   * @return a {@code Value} which has the same content of this value
   */
  Value<?> copyWith(String name);

  /**
   * Accepts a {@link ValueVisitor} to be able to be traversed
   *
   * @param v a visitor class used for traversing {@code Value}s
   */
  void accept(ValueVisitor v);

  /**
   * Returns the content of this {@code Value} as a boolean type
   *
   * @return the content of this {@code Value}
   */
  default boolean getAsBoolean() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content of this {@code Value} as a integer type
   *
   * @return the content of this {@code Value}
   */
  default int getAsInt() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content of this {@code Value} as a long type
   *
   * @return the content of this {@code Value}
   */
  default long getAsLong() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content of this {@code Value} as a float type
   *
   * @return the content of this {@code Value}
   */
  default float getAsFloat() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content of this {@code Value} as a double type
   *
   * @return the content of this {@code Value}
   */
  default double getAsDouble() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content of this {@code Value} as a string
   *
   * @return the content of this {@code Value}
   */
  default Optional<String> getAsString() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content of this {@code Value} as a byte array type
   *
   * @return the content of this {@code Value}
   */
  default Optional<byte[]> getAsBytes() {
    throw new UnsupportedOperationException();
  }
}
