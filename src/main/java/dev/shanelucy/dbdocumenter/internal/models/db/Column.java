package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Represents a database column with metadata for PlantUML ERD generation.
 *
 * <p>Encapsulates column-level information relevant to an ERD: name, data type, maximum length,
 * nullability, and structural constraints ({@link Constraint#FK}, {@link Constraint#UNIQUE}).
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> All parameters are validated in the compact constructor. The constraints
 * list is defensively copied to prevent external modification.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * Column emailColumn = Column.builder()
 *     .name("email")
 *     .dataType("varchar")
 *     .maximumLength(255)
 *     .isNullable(true)
 *     .constraints(List.of(Constraint.UNIQUE))
 *     .build();
 *
 * Column idColumn = Column.builder()
 *     .name("id")
 *     .dataType("uuid")
 *     .isNullable(false)
 *     .build();
 * }</pre>
 *
 * @param name identifier used in the database schema
 * @param dataType the SQL data type (e.g., "varchar", "integer", "uuid")
 * @param maximumLength character limit for varchar/char types; 0 for fixed-size types
 * @param isNullable true if the column permits NULL values; false otherwise
 * @param constraints the {@link List} of {@link Constraint} values applied to this column — never
 *     null, may be empty
 * @see Constraint
 * @see Table
 */
public record Column(
    String name,
    String dataType,
    int maximumLength,
    boolean isNullable,
    List<Constraint> constraints) {

  public Column {
    Validators.isNotBlank(name, "name");
    Validators.isNotBlank(dataType, "dataType");
    Validators.containsNoNullElements(constraints, "constraints");
    constraints = List.copyOf(constraints);
  }

  /**
   * Creates a new Column with a different data type, preserving all other properties.
   *
   * <p>Follows the "wither" pattern common in immutable objects, returning a new instance rather
   * than modifying the existing one.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column original = Column.builder()
   *     .name("status")
   *     .dataType("USER-DEFINED")
   *     .isNullable(true)
   *     .build();
   *
   * Column resolved = original.withDataType("order_status");
   * // resolved has dataType="order_status", all other properties unchanged
   * }</pre>
   *
   * @param newDataType the new data type for the column
   * @return a new Column instance with the updated data type
   */
  public Column withDataType(final String newDataType) {
    return new Column(
        this.name, newDataType, this.maximumLength, this.isNullable, this.constraints);
  }

  /**
   * Creates a new Column with different constraints, preserving all other properties.
   *
   * @param newConstraints the replacement constraints (defensively copied)
   * @return a new Column instance with the updated constraints
   */
  public Column withConstraints(final List<Constraint> newConstraints) {
    return new Column(
        this.name, this.dataType, this.maximumLength, this.isNullable, newConstraints);
  }

  /**
   * Creates a new builder for constructing Column instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link Column} instances using a fluent API.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column column = Column.builder()
   *     .name("user_id")
   *     .dataType("bigint")
   *     .isNullable(false)
   *     .constraints(List.of(Constraint.FK))
   *     .build();
   * }</pre>
   *
   * @see Column
   */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable String dataType;
    private int maximumLength;
    private @Nullable Boolean isNullable;
    private List<Constraint> constraints = List.of();

    /**
     * Sets the column name.
     *
     * @param name the column name
     * @return this builder instance for method chaining
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the SQL data type for this column.
     *
     * <p><b>Examples:</b> "varchar", "integer", "uuid", "bigint", "timestamp", "order_status" (enum
     * type)
     *
     * @param dataType the SQL data type
     * @return this builder instance for method chaining
     */
    public Builder dataType(final String dataType) {
      this.dataType = dataType;
      return this;
    }

    /**
     * Sets the maximum length for character-based data types.
     *
     * <p><b>Note:</b> Only relevant for character types like VARCHAR. For other types this value is
     * typically 0.
     *
     * @param maximumLength the maximum length (0 for non-character types)
     * @return this builder instance for method chaining
     */
    public Builder maximumLength(final int maximumLength) {
      this.maximumLength = maximumLength;
      return this;
    }

    /**
     * Sets whether the column permits NULL values.
     *
     * @param isNullable true if the column is nullable
     * @return this builder instance for method chaining
     */
    public Builder isNullable(final boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    /**
     * Sets the constraints for this column.
     *
     * @param constraints the {@link List} of {@link Constraint} values to apply (defensively
     *     copied)
     * @return this builder instance for method chaining
     */
    public Builder constraints(final List<Constraint> constraints) {
      this.constraints = constraints;
      return this;
    }

    /**
     * Builds and returns a new {@link Column} instance.
     *
     * @return a new immutable {@link Column} instance
     * @throws ValidationException if any required field is null or blank, including {@code
     *     isNullable}, which has no default so a forgotten call cannot silently change FK
     *     cardinality
     */
    public Column build() {
      return new Column(
          Validators.isNotBlank(name, "name"),
          Validators.isNotBlank(dataType, "dataType"),
          maximumLength,
          Validators.isNotNull(isNullable, "isNullable"),
          constraints);
    }
  }
}
