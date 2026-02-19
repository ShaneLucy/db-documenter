package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;

/**
 * Represents a database column with metadata for PlantUML entity-relationship diagram generation.
 *
 * <p>This record encapsulates all column-level information including name, data type, maximum
 * length, and associated constraints (e.g., UNIQUE, CHECK, DEFAULT, AUTO_INCREMENT, NULLABLE).
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> All parameters are validated as non-null in the compact constructor. The
 * constraints list is defensively copied to prevent external modification.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Create a column with constraints using the builder
 * Column emailColumn = Column.builder()
 *     .name("email")
 *     .dataType("varchar")
 *     .maximumLength(255)
 *     .constraints(List.of(Constraint.UNIQUE, Constraint.NULLABLE))
 *     .build();
 *
 * // Create a simple column without constraints
 * Column idColumn = Column.builder()
 *     .name("id")
 *     .dataType("uuid")
 *     .build();
 * }</pre>
 *
 * @param name identifier used in the database schema
 * @param dataType the SQL data type (e.g., "varchar", "integer", "uuid")
 * @param maximumLength character limit for varchar/char types; 0 for fixed-size types
 * @param constraints the {@link List} of {@link Constraint} values applied to this column - never
 *     null, may be empty
 * @see Constraint
 * @see Table
 */
public record Column(
    String name, String dataType, int maximumLength, List<Constraint> constraints) {

  public Column {
    Validators.isNotBlank(name, "name");
    Validators.isNotBlank(dataType, "dataType");
    Validators.isNotNull(constraints, "constraints");
    constraints = List.copyOf(constraints);
  }

  /**
   * Checks if this column allows null values.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column column = Column.builder()
   *     .name("email")
   *     .constraints(List.of(Constraint.NULLABLE))
   *     .build();
   *
   * boolean nullable = column.isNullable(); // Returns true
   * }</pre>
   *
   * @return true if the column has the NULLABLE constraint, false otherwise
   */
  public boolean isNullable() {
    return constraints.contains(Constraint.NULLABLE);
  }

  /**
   * Creates a new Column with a different data type, preserving all other properties.
   *
   * <p>This method follows the "wither" pattern common in immutable objects, returning a new
   * instance rather than modifying the existing one.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column original = Column.builder()
   *     .name("status")
   *     .dataType("USER-DEFINED")
   *     .constraints(List.of(Constraint.NULLABLE))
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
    return new Column(this.name, newDataType, this.maximumLength, this.constraints);
  }

  /**
   * Creates a new builder for constructing Column instances.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column column = Column.builder()
   *     .name("user_id")
   *     .dataType("integer")
   *     .build();
   * }</pre>
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link Column} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column column = Column.builder()
   *     .name("user_id")
   *     .dataType("bigint")
   *     .maximumLength(0)
   *     .constraints(List.of(Constraint.FK, Constraint.NULLABLE))
   *     .build();
   * }</pre>
   *
   * @see Column
   */
  public static final class Builder {
    private String name;
    private String dataType;
    private int maximumLength;
    private List<Constraint> constraints;

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
     * <p><b>Note:</b> This is only relevant for character types like VARCHAR. For other types, this
     * value is typically 0.
     *
     * @param maximumLength the maximum length (0 for non-character types)
     * @return this builder instance for method chaining
     */
    public Builder maximumLength(final int maximumLength) {
      this.maximumLength = maximumLength;
      return this;
    }

    /**
     * Sets the constraints for this column.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param constraints the {@link List} of {@link Constraint} values to apply (defensively
     *     copied)
     * @return this builder instance for method chaining
     */
    public Builder constraints(final List<Constraint> constraints) {
      this.constraints = List.copyOf(constraints);
      return this;
    }

    /**
     * Builds and returns a new {@link Column} instance.
     *
     * @return a new immutable {@link Column} instance
     * @throws ValidationException if any required field is null
     */
    public Column build() {
      return new Column(name, dataType, maximumLength, constraints);
    }
  }
}
