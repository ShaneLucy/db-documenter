package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Represents a database table with its columns, primary key, and foreign key relationships.
 *
 * <p>This record is the central model for table metadata, containing all information needed to
 * document a table's structure and relationships.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. Both columns and foreignKeys
 * lists are defensively copied to prevent external modification.
 *
 * <p><b>Validation:</b> All parameters are validated as non-null in the compact constructor. Note
 * that while the {@code primaryKey} Optional itself must be non-null, it may be empty for tables
 * without primary keys (e.g., log tables).
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Table with primary key
 * Table usersTable = Table.builder()
 *     .name("users")
 *     .columns(List.of(idColumn, nameColumn))
 *     .primaryKey(primaryKey)
 *     .foreignKeys(List.of())
 *     .build();
 *
 * // Table without primary key (e.g., log table)
 * Table logTable = Table.builder()
 *     .name("audit_log")
 *     .columns(List.of(timestampColumn, actionColumn))
 *     .primaryKey(null)  // Builder wraps in Optional.ofNullable()
 *     .foreignKeys(List.of())
 *     .build();
 * }</pre>
 *
 * @param name the table name
 * @param columns the {@link List} of {@link Column} instances in this table (defensively copied)
 * @param primaryKey the {@link Optional} {@link PrimaryKey} constraint, or empty if the table has
 *     no primary key
 * @param foreignKeys the {@link List} of {@link ForeignKey} relationships (defensively copied)
 * @see Column
 * @see PrimaryKey
 * @see ForeignKey
 */
public record Table(
    @NonNull String name,
    @NonNull List<Column> columns,
    @NonNull Optional<PrimaryKey> primaryKey,
    @NonNull List<ForeignKey> foreignKeys) {

  public Table {
    Validators.isNotNull(name, "name");
    Validators.isNotNull(columns, "columns");
    Validators.isNotNull(primaryKey, "primaryKey");
    Validators.isNotNull(foreignKeys, "foreignKeys");
    columns = List.copyOf(columns);
    foreignKeys = List.copyOf(foreignKeys);
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
  public static class Builder {
    private String name;
    private List<Column> columns;
    private PrimaryKey primaryKey;
    private List<ForeignKey> foreignKeys;

    /**
     * Sets the table name.
     *
     * @param name the table name
     * @return this builder instance for method chaining
     */
    public Builder name(final @NonNull String name) {
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
    public Builder columns(final @NonNull List<Column> columns) {
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
    public Builder foreignKeys(final @NonNull List<ForeignKey> foreignKeys) {
      this.foreignKeys = List.copyOf(foreignKeys);
      return this;
    }

    /**
     * Builds and returns a new {@link Table} instance.
     *
     * <p><b>Note:</b> The primaryKey is wrapped in {@link Optional#ofNullable(Object)} to handle
     * tables without primary keys.
     *
     * @return a new immutable {@link Table} instance
     * @throws ValidationException if any required field is null
     */
    public Table build() {
      return new Table(name, columns, Optional.ofNullable(primaryKey), foreignKeys);
    }
  }
}
