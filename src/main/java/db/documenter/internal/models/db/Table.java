package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Represents a database table with its columns, primary key, foreign key relationships, and
 * optional partition metadata.
 *
 * <p>This record is the central model for table metadata, containing all information needed to
 * document a table's structure and relationships. Partitioned tables carry the partition strategy
 * expression and an ordered list of child partition names for diagram rendering.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. Both {@code columns}, {@code
 * foreignKeys}, and {@code partitionNames} lists are defensively copied to prevent external
 * modification.
 *
 * <p><b>Validation:</b> All collection parameters are validated as non-null in the compact
 * constructor. The {@code primaryKey} Optional itself must be non-null but may be empty for tables
 * without primary keys. The {@code partitionStrategy} is nullable; a non-null value indicates a
 * partitioned table.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Regular table with primary key
 * Table usersTable = Table.builder()
 *     .name("users")
 *     .columns(List.of(idColumn, nameColumn))
 *     .primaryKey(primaryKey)
 *     .foreignKeys(List.of())
 *     .build();
 *
 * // Partitioned table with child partition names
 * Table statsTable = Table.builder()
 *     .name("daily_project_stats")
 *     .columns(List.of(idColumn, statDateColumn))
 *     .primaryKey(primaryKey)
 *     .foreignKeys(List.of())
 *     .partitionStrategy("RANGE (stat_date)")
 *     .partitionNames(List.of("daily_project_stats_2024_q3", "daily_project_stats_2024_q4"))
 *     .build();
 * }</pre>
 *
 * @param name the table name; must not be blank
 * @param columns the {@link List} of {@link Column} instances in this table (defensively copied)
 * @param primaryKey the {@link Optional} {@link PrimaryKey} constraint, or empty if the table has
 *     no primary key
 * @param foreignKeys the {@link List} of {@link ForeignKey} relationships (defensively copied)
 * @param partitionStrategy the PostgreSQL partition key expression (e.g., {@code "RANGE
 *     (stat_date)"}), or {@code null} for non-partitioned tables
 * @param partitionNames ordered list of child partition names for partitioned tables (defensively
 *     copied); empty for non-partitioned tables
 * @see Column
 * @see PrimaryKey
 * @see ForeignKey
 */
public record Table(
    String name,
    List<Column> columns,
    Optional<PrimaryKey> primaryKey,
    List<ForeignKey> foreignKeys,
    @Nullable String partitionStrategy,
    List<String> partitionNames) {

  public Table {
    Validators.isNotBlank(name, "name");
    Validators.isNotNull(columns, "columns");
    Validators.isNotNull(primaryKey, "primaryKey");
    Validators.isNotNull(foreignKeys, "foreignKeys");
    Validators.isNotNull(partitionNames, "partitionNames");
    columns = List.copyOf(columns);
    foreignKeys = List.copyOf(foreignKeys);
    partitionNames = List.copyOf(partitionNames);
  }

  /**
   * Creates a new builder for constructing Table instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link Table} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Table table = Table.builder()
   *     .name("products")
   *     .columns(List.of(idColumn, nameColumn, priceColumn))
   *     .primaryKey(primaryKey)
   *     .foreignKeys(List.of(categoryFk))
   *     .build();
   * }</pre>
   *
   * @see Table
   */
  public static final class Builder {
    private String name;
    private List<Column> columns;
    private PrimaryKey primaryKey;
    private List<ForeignKey> foreignKeys;
    private @Nullable String partitionStrategy;
    private List<String> partitionNames;

    /**
     * Sets the table name.
     *
     * @param name the table name
     * @return this builder instance for method chaining
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the columns in this table.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param columns the {@link List} of {@link Column} instances (defensively copied)
     * @return this {@link Builder} instance for method chaining
     */
    public Builder columns(final List<Column> columns) {
      this.columns = List.copyOf(columns);
      return this;
    }

    /**
     * Sets the primary key for this table.
     *
     * <p><b>Note:</b> This parameter may be null for tables without primary keys. The {@link
     * #build()} method will wrap it in an {@link Optional}.
     *
     * @param primaryKey the {@link PrimaryKey}, or null if the table has no primary key
     * @return this {@link Builder} instance for method chaining
     */
    public Builder primaryKey(final PrimaryKey primaryKey) {
      this.primaryKey = primaryKey;
      return this;
    }

    /**
     * Sets the foreign key relationships for this table.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param foreignKeys the {@link List} of {@link ForeignKey} relationships (defensively copied)
     * @return this {@link Builder} instance for method chaining
     */
    public Builder foreignKeys(final List<ForeignKey> foreignKeys) {
      this.foreignKeys = List.copyOf(foreignKeys);
      return this;
    }

    /**
     * Sets the partition strategy expression for partitioned tables.
     *
     * @param partitionStrategy the PostgreSQL partition key expression (e.g., {@code "RANGE
     *     (stat_date)"}), or {@code null} for non-partitioned tables
     * @return this {@link Builder} instance for method chaining
     */
    public Builder partitionStrategy(final @Nullable String partitionStrategy) {
      this.partitionStrategy = partitionStrategy;
      return this;
    }

    /**
     * Sets the ordered list of child partition names.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param partitionNames ordered child partition names (defensively copied); pass an empty list
     *     for non-partitioned tables
     * @return this {@link Builder} instance for method chaining
     */
    public Builder partitionNames(final List<String> partitionNames) {
      this.partitionNames = List.copyOf(partitionNames);
      return this;
    }

    /**
     * Builds and returns a new {@link Table} instance.
     *
     * <p><b>Note:</b> The {@code primaryKey} is wrapped in {@link Optional#ofNullable(Object)} to
     * handle tables without primary keys. If {@code partitionNames} was not set, it defaults to an
     * empty list.
     *
     * @return a new immutable {@link Table} instance
     * @throws ValidationException if any required field is null or blank
     */
    public Table build() {
      return new Table(
          name,
          columns,
          Optional.ofNullable(primaryKey),
          foreignKeys,
          partitionStrategy,
          partitionNames == null ? List.of() : partitionNames);
    }
  }
}
