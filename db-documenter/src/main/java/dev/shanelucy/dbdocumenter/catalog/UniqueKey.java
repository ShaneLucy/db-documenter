package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A unique key of a {@link Table}, whether declared as a constraint or as a unique index.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param name the constraint or index name; empty when the vendor does not expose one. When
 *     present, not empty.
 * @param columns the key's column names in key order (not table order); at least one, no nulls, no
 *     duplicates; defensively copied
 */
public record UniqueKey(Optional<String> name, List<String> columns) {

  // CPD-OFF: deliberately parallel to PrimaryKey; the types are kept separate so a table can have
  // at most one primary key, and sharing a builder base would leak a non-public type into the API.

  /**
   * Validates and copies the components.
   *
   * @throws IllegalArgumentException if a component is null, the name is empty, or the columns are
   *     empty or contain a null, empty or duplicate name
   */
  public UniqueKey {
    CatalogValidation.validateOptionalName(name, "name");
    CatalogValidation.validateColumnNames(columns, "columns");
    Validators.containsNoDuplicates(columns, "columns");
    columns = List.copyOf(columns);
  }

  /** Creates a new builder for {@link UniqueKey}. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing {@link UniqueKey} instances using a fluent API. */
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
     * Builds the {@link UniqueKey}; the columns must have been set.
     *
     * @throws IllegalArgumentException if the columns are unset or the key is invalid
     */
    public UniqueKey build() {
      return new UniqueKey(Optional.ofNullable(name), Validators.isNotNull(columns, "columns"));
    }
  }
  // CPD-ON
}
