package com.scalar.db.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.scalar.db.api.Scan.Ordering.Order;
import com.scalar.db.io.DataType;
import com.scalar.db.util.ImmutableLinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A class that represents the table metadata */
public class TableMetadata {

  private final LinkedHashSet<String> columnNames;
  private final Map<String, DataType> columnDataTypes;
  private final LinkedHashSet<String> partitionKeyNames;
  private final LinkedHashSet<String> clusteringKeyNames;
  private final Map<String, Order> clusteringOrders;
  private final Set<String> secondaryIndexNames;

  private TableMetadata(
      LinkedHashMap<String, DataType> columns,
      LinkedHashSet<String> partitionKeyNames,
      LinkedHashSet<String> clusteringKeyNames,
      Map<String, Order> clusteringOrders,
      Set<String> secondaryIndexNames) {
    this.columnNames = new ImmutableLinkedHashSet<>(Objects.requireNonNull(columns.keySet()));
    this.columnDataTypes = ImmutableMap.copyOf(Objects.requireNonNull(columns));
    this.partitionKeyNames =
        new ImmutableLinkedHashSet<>(Objects.requireNonNull(partitionKeyNames));
    this.clusteringKeyNames =
        new ImmutableLinkedHashSet<>(Objects.requireNonNull(clusteringKeyNames));
    this.clusteringOrders = ImmutableMap.copyOf(Objects.requireNonNull(clusteringOrders));
    this.secondaryIndexNames = ImmutableSet.copyOf(Objects.requireNonNull(secondaryIndexNames));
  }

  /**
   * Returns the column names
   *
   * @return an {@code LinkedHashSet} of column names
   */
  public LinkedHashSet<String> getColumnNames() {
    return columnNames;
  }

  /**
   * Returns the data type of the specified column
   *
   * @param columnName a column name to retrieve the data type
   * @return an {@code DataType} of the specified column
   */
  public DataType getColumnDataType(String columnName) {
    return columnDataTypes.get(columnName);
  }

  /**
   * Returns the partition key names
   *
   * @return an {@code LinkedHashSet} of partition key names
   */
  public LinkedHashSet<String> getPartitionKeyNames() {
    return partitionKeyNames;
  }

  /**
   * Returns the clustering key names
   *
   * @return an {@code LinkedHashSet} of clustering key names
   */
  public LinkedHashSet<String> getClusteringKeyNames() {
    return clusteringKeyNames;
  }

  /**
   * Returns the specified clustering order
   *
   * @param clusteringKeyName a clustering key name to retrieve the order
   * @return an {@code Scan.Ordering.Order} of the specified clustering key
   */
  public Scan.Ordering.Order getClusteringOrder(String clusteringKeyName) {
    return clusteringOrders.get(clusteringKeyName);
  }

  /**
   * Returns the secondary index names
   *
   * @return an {@code Set} of secondary index names
   */
  public Set<String> getSecondaryIndexNames() {
    return secondaryIndexNames;
  }

  /**
   * Creates a new builder instance
   *
   * @return a new builder instance
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** A builder class that creates a TableMetadata instance */
  public static final class Builder {
    private final LinkedHashMap<String, DataType> columns = new LinkedHashMap<>();
    private final LinkedHashSet<String> partitionKeyNames = new LinkedHashSet<>();
    private final LinkedHashSet<String> clusteringKeyNames = new LinkedHashSet<>();
    private final Map<String, Order> clusteringOrders = new HashMap<>();
    private final Set<String> secondaryIndexNames = new HashSet<>();

    private Builder() {}

    public Builder addColumn(String name, DataType type) {
      columns.put(name, type);
      return this;
    }

    public Builder addPartitionKey(String name) {
      partitionKeyNames.add(name);
      return this;
    }

    public Builder addClusteringKey(String name) {
      addClusteringKey(name, Scan.Ordering.Order.ASC);
      return this;
    }

    public Builder addClusteringKey(String name, Scan.Ordering.Order clusteringOrder) {
      clusteringKeyNames.add(name);
      clusteringOrders.put(name, clusteringOrder);
      return this;
    }

    public Builder addSecondaryIndex(String name) {
      secondaryIndexNames.add(name);
      return this;
    }

    public TableMetadata build() {
      if (columns.isEmpty()) {
        throw new IllegalStateException("need to specify one or more columns");
      }
      if (partitionKeyNames.isEmpty()) {
        throw new IllegalStateException("need to specify one or more partition keys");
      }
      partitionKeyNames.forEach(
          k -> {
            if (!columns.containsKey(k)) {
              throw new IllegalStateException(
                  "need tp specify the column definition of "
                      + k
                      + " specified as a partition key");
            }
          });
      clusteringKeyNames.forEach(
          k -> {
            if (!columns.containsKey(k)) {
              throw new IllegalStateException(
                  "need tp specify the column definition of "
                      + k
                      + " specified as a clustering key");
            }
          });
      return new TableMetadata(
          columns, partitionKeyNames, clusteringKeyNames, clusteringOrders, secondaryIndexNames);
    }
  }
}
