package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import org.jspecify.annotations.Nullable;

/**
 * A column of a {@link Table}.
 *
 * <p>There is no ordinal: a column's position in {@link Table#columns()} is its position in the
 * table.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param name the column name as stored in the catalog; not empty
 * @param nullable whether a stored value can be {@code NULL}. This can differ from the vendor's
 *     declared flag (a SQLite rowid alias is reported NOT NULL), and it can be {@code true} for a
 *     primary-key column (SQLite allows {@code NULL} in a non-integer primary key of a rowid
 *     table), so do not assume that a primary-key column is NOT NULL.
 * @param dataType the column's data type
 */
public record Column(String name, boolean nullable, DataType dataType) {

  /**
   * Validates the components.
   *
   * @throws IllegalArgumentException if the name is null or empty, or the data type is null
   */
  public Column {
    Validators.isNotEmpty(name, "name");
    Validators.isNotNull(dataType, "dataType");
  }

  /** Creates a new builder for {@link Column}. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link Column} instances using a fluent API.
   *
   * <p>{@code nullable} defaults to {@code true}, the SQL DDL default; the name and data type are
   * required.
   */
  public static final class Builder {
    private @Nullable String name;
    private boolean nullable = true;
    private @Nullable DataType dataType;

    /** Returns this builder for method chaining. */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder nullable(final boolean nullable) {
      this.nullable = nullable;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder dataType(final DataType dataType) {
      this.dataType = dataType;
      return this;
    }

    /**
     * Builds the {@link Column}; the name and data type must have been set.
     *
     * @throws IllegalArgumentException if the name or data type is unset, or the name is empty
     */
    public Column build() {
      return new Column(
          Validators.isNotNull(name, "name"), nullable, Validators.isNotNull(dataType, "dataType"));
    }
  }
}
