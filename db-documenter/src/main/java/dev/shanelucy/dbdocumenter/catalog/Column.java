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
 * @param nullable whether the column accepts {@code NULL}
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
   * <p>Every property is required, including {@code nullable}, so that a reader cannot silently
   * report a column with the wrong nullability.
   */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable Boolean nullable;
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
     * Builds the {@link Column}; every property must have been set.
     *
     * @throws IllegalArgumentException if a property is unset or the name is empty
     */
    public Column build() {
      return new Column(
          Validators.isNotNull(name, "name"),
          Boolean.TRUE.equals(Validators.isNotNull(nullable, "nullable")),
          Validators.isNotNull(dataType, "dataType"));
    }
  }
}
