package com.scalar.db.storage.common;

import com.google.common.base.MoreObjects;
import com.scalar.db.api.Result;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.io.Key;
import com.scalar.db.io.Value;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultImpl implements Result {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResultImpl.class);

  private final Map<String, Value<?>> values;
  private final TableMetadata metadata;

  public ResultImpl(Map<String, Value<?>> values, TableMetadata metadata) {
    this.values = Objects.requireNonNull(values);
    this.metadata = Objects.requireNonNull(metadata);
  }

  @Override
  public Optional<Key> getPartitionKey() {
    return getKey(metadata.getPartitionKeyNames());
  }

  @Override
  public Optional<Key> getClusteringKey() {
    return getKey(metadata.getClusteringKeyNames());
  }

  private Optional<Key> getKey(LinkedHashSet<String> names) {
    List<Value<?>> list = new ArrayList<>();
    for (String name : names) {
      Value<?> value = values.get(name);
      if (value == null) {
        LOGGER.warn("full key doesn't seem to be projected into the result");
        return Optional.empty();
      }
      list.add(value);
    }
    return Optional.of(new Key(list));
  }

  @Override
  public Optional<Value<?>> getValue(String name) {
    return Optional.ofNullable(values.get(name));
  }

  @Override
  @Nonnull
  public Map<String, Value<?>> getValues() {
    return Collections.unmodifiableMap(values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(values);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ResultImpl)) {
      return false;
    }
    ResultImpl other = (ResultImpl) o;
    return this.values.equals(other.values);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("values", values).toString();
  }
}
