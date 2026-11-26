package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * A table and its keys.
 *
 * <p><b>Ordering:</b> columns keep the given order, which is the table's column order. Unique keys
 * are sorted by column list, then name (absent first). Foreign keys are sorted by local columns,
 * referenced table, referenced columns, {@code onDelete}, {@code onUpdate}, then name (absent
 * first). Column lists are compared element by element, with a shorter prefix first.
 *
 * <p><b>Validation:</b> column names are unique, and every key's local columns are columns of this
 * table. Referenced tables are not checked, since foreign keys may dangle.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param name the schema-qualified table name
 * @param columns the columns in table order; may be empty, since some vendors allow zero-column
 *     tables; defensively copied
 * @param primaryKey the primary key, if the table has one
 * @param uniqueKeys the unique keys; defensively copied and sorted
 * @param foreignKeys the foreign keys declared on this table; defensively copied and sorted
 */
public record Table(
    QualifiedName name,
    List<Column> columns,
    Optional<PrimaryKey> primaryKey,
    List<UniqueKey> uniqueKeys,
    List<ForeignKey> foreignKeys) {

  /**
   * Validates, copies and sorts the components.
   *
   * @throws IllegalArgumentException if a component is null or contains a null element, two columns
   *     share a name, or a key refers to a column not in this table
   */
  public Table {
    Validators.isNotNull(name, "name");
    Validators.containsNoNullElements(columns, "columns");
    Validators.isNotNull(primaryKey, "primaryKey");
    Validators.containsNoNullElements(uniqueKeys, "uniqueKeys");
    Validators.containsNoNullElements(foreignKeys, "foreignKeys");
    CatalogValidation.validateUniqueKeys(columns, Column::name, "columns.name");
    validateKeyColumns(columns, primaryKey, uniqueKeys, foreignKeys);
    columns = List.copyOf(columns);
    uniqueKeys = List.copyOf(CatalogOrder.sortedCopy(uniqueKeys, CatalogOrder.UNIQUE_KEYS));
    foreignKeys = List.copyOf(CatalogOrder.sortedCopy(foreignKeys, CatalogOrder.FOREIGN_KEYS));
  }

  /** Creates a new builder for {@link Table}. */
  public static Builder builder() {
    return new Builder();
  }

  private static void validateKeyColumns(
      final List<Column> columns,
      final Optional<PrimaryKey> primaryKey,
      final List<UniqueKey> uniqueKeys,
      final List<ForeignKey> foreignKeys) {
    final Set<String> columnNames = columns.stream().map(Column::name).collect(Collectors.toSet());
    primaryKey.ifPresent(
        key -> CatalogValidation.validateColumnsExist(key.columns(), columnNames, "primaryKey"));
    uniqueKeys.forEach(
        key -> CatalogValidation.validateColumnsExist(key.columns(), columnNames, "uniqueKeys"));
    foreignKeys.forEach(
        key -> CatalogValidation.validateColumnsExist(key.columns(), columnNames, "foreignKeys"));
  }

  /**
   * Builder for constructing {@link Table} instances using a fluent API.
   *
   * <p>Only the name is required; the lists default to empty and the primary key to absent.
   */
  public static final class Builder {
    private @Nullable QualifiedName name;
    private List<Column> columns = List.of();
    private @Nullable PrimaryKey primaryKey;
    private List<UniqueKey> uniqueKeys = List.of();
    private List<ForeignKey> foreignKeys = List.of();

    /** Returns this builder for method chaining. */
    public Builder name(final QualifiedName name) {
      this.name = name;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder columns(final List<Column> columns) {
      this.columns = columns;
      return this;
    }

    /** Returns this builder for method chaining; leave unset for a table without one. */
    public Builder primaryKey(final @Nullable PrimaryKey primaryKey) {
      this.primaryKey = primaryKey;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder uniqueKeys(final List<UniqueKey> uniqueKeys) {
      this.uniqueKeys = uniqueKeys;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder foreignKeys(final List<ForeignKey> foreignKeys) {
      this.foreignKeys = foreignKeys;
      return this;
    }

    /**
     * Builds the {@link Table}; the name must have been set.
     *
     * @throws IllegalArgumentException if the name is unset or the table is invalid
     */
    public Table build() {
      return new Table(
          Validators.isNotNull(name, "name"),
          columns,
          Optional.ofNullable(primaryKey),
          uniqueKeys,
          foreignKeys);
    }
  }
}
