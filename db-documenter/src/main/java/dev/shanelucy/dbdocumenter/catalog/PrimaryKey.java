package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The primary key of a {@link Table}.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param name the constraint name; empty when the vendor exposes none, or only a fixed placeholder
 *     (SQLite, and MySQL's {@code PRIMARY}). When present, not empty, and kept verbatim even when
 *     the vendor generated it.
 * @param columns the key's column names in key order (not table order); at least one, no nulls, no
 *     duplicates; defensively copied
 */
public record PrimaryKey(Optional<String> name, List<String> columns) {

  /**
   * Validates and copies the components.
   *
   * @throws IllegalArgumentException if a component is null, the name is empty, or the columns are
   *     empty or contain a null, empty or duplicate name
   */
  public PrimaryKey {
    CatalogValidation.validateOptionalName(name, "name");
    CatalogValidation.validateColumnNames(columns, "columns");
    Validators.containsNoDuplicates(columns, "columns");
    columns = List.copyOf(columns);
  }

  /** Creates a new builder for {@link PrimaryKey}. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing {@link PrimaryKey} instances using a fluent API. */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable List<String> columns;

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

    /**
     * Builds the {@link PrimaryKey}; the columns must have been set.
     *
     * @throws IllegalArgumentException if the columns are unset or the key is invalid
     */
    public PrimaryKey build() {
      return new PrimaryKey(Optional.ofNullable(name), Validators.isNotNull(columns, "columns"));
    }
  }
}
