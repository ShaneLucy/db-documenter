package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A foreign key from a {@link Table} to a referenced table.
 *
 * <p>{@code columns} and {@code referencedColumns} pair up by position, in key order: the first
 * local column references the first referenced column, and so on. Neither list is ever sorted.
 *
 * <p>The model is deliberately permissive, because vendors are: {@code columns} may repeat a column
 * (SQLite accepts {@code FOREIGN KEY (a, a)}), and {@code referencedColumns} need not form a
 * primary or unique key of the referenced table (MySQL and MariaDB allow any indexed columns).
 *
 * <p>The referenced table may be absent from the snapshot, for example when it is in a schema that
 * was not read, the user cannot see it, or the vendor allows dangling references.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param name the constraint name; empty when the vendor exposes none (SQLite). When present, not
 *     empty, and kept verbatim even when the vendor generated it.
 * @param columns the referencing column names in the owning table, in key order; at least one, no
 *     nulls; defensively copied
 * @param referencedTable the table the key references
 * @param referencedColumns the referenced column names, paired with {@code columns} by position;
 *     the same size as {@code columns}, no nulls; defensively copied
 * @param onDelete the action taken when a referenced row is deleted
 * @param onUpdate the action taken when a referenced key is updated
 */
public record ForeignKey(
    Optional<String> name,
    List<String> columns,
    QualifiedName referencedTable,
    List<String> referencedColumns,
    ReferentialAction onDelete,
    ReferentialAction onUpdate) {

  /**
   * Validates and copies the components.
   *
   * @throws IllegalArgumentException if a component is null, the name is empty, a column list is
   *     empty or contains a null or empty name, or the column lists differ in size
   */
  public ForeignKey {
    CatalogValidation.validateOptionalName(name, "name");
    CatalogValidation.validateColumnNames(columns, "columns");
    Validators.isNotNull(referencedTable, "referencedTable");
    CatalogValidation.validateColumnNames(referencedColumns, "referencedColumns");
    if (columns.size() != referencedColumns.size()) {
      throw new ValidationException(
          "columns and referencedColumns must have the same size, but have "
              + columns.size()
              + " and "
              + referencedColumns.size());
    }
    Validators.isNotNull(onDelete, "onDelete");
    Validators.isNotNull(onUpdate, "onUpdate");
    columns = List.copyOf(columns);
    referencedColumns = List.copyOf(referencedColumns);
  }

  /** Creates a new builder for {@link ForeignKey}. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link ForeignKey} instances using a fluent API.
   *
   * <p>{@code onDelete} and {@code onUpdate} default to {@link ReferentialAction#NO_ACTION}, the
   * SQL default.
   */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable List<String> columns;
    private @Nullable QualifiedName referencedTable;
    private @Nullable List<String> referencedColumns;
    private ReferentialAction onDelete = ReferentialAction.NO_ACTION;
    private ReferentialAction onUpdate = ReferentialAction.NO_ACTION;

    /** Returns this builder for method chaining; leave unset when the name is unknown. */
    public Builder name(final @Nullable String name) {
      this.name = name;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder columns(final List<String> columns) {
      this.columns = columns;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder referencedTable(final QualifiedName referencedTable) {
      this.referencedTable = referencedTable;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder referencedColumns(final List<String> referencedColumns) {
      this.referencedColumns = referencedColumns;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder onDelete(final ReferentialAction onDelete) {
      this.onDelete = onDelete;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder onUpdate(final ReferentialAction onUpdate) {
      this.onUpdate = onUpdate;
      return this;
    }

    /**
     * Builds the {@link ForeignKey}; the columns, referenced table and referenced columns must have
     * been set.
     *
     * @throws IllegalArgumentException if a required property is unset or the key is invalid
     */
    public ForeignKey build() {
      return new ForeignKey(
          Optional.ofNullable(name),
          Validators.isNotNull(columns, "columns"),
          Validators.isNotNull(referencedTable, "referencedTable"),
          Validators.isNotNull(referencedColumns, "referencedColumns"),
          onDelete,
          onUpdate);
    }
  }
}
