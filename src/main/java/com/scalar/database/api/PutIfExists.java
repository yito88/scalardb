package com.scalar.database.api;

import com.scalar.database.io.Value;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * An optional parameter of {@link Put} for conditional put. {@link Put} with this condition makes
 * the operation take effect only if a specified entry exists.
 *
 * @author Hiroyuki Yamada
 */
@Immutable
public class PutIfExists implements MutationCondition {

  public PutIfExists() {}

  /**
   * Returns an empty list of {@link Value}s since any conditions are not given to this object.
   *
   * @return an empty list of {@code Value}s
   */
  @Override
  @Nonnull
  public List<ConditionalExpression> getExpressions() {
    return Collections.emptyList();
  }

  @Override
  public void accept(MutationConditionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if:
   *
   * <ul>
   *   <li>it is also an {@code PutIfExists}
   * </ul>
   *
   * @param o an object to be tested for equality
   * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof PutIfExists)) {
      return false;
    }
    return true;
  }
}
