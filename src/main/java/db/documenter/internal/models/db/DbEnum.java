package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;

/**
 * Represents a database-specific enum type definition for PlantUML diagram generation.
 *
 * <p>This record captures custom enum types created in the database (e.g., PostgreSQL {@code CREATE
 * TYPE} enums) and their associated column mappings. These are rendered as PlantUML enum notation
 * in the generated diagrams.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. The enumValues list is
 * defensively copied to prevent external modification.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // PostgreSQL: CREATE TYPE order_status AS ENUM ('PENDING', 'SHIPPED', 'DELIVERED');
 * DbEnum orderStatus = DbEnum.builder()
 *     .enumName("order_status")
 *     .columnName("status")
 *     .enumValues(List.of("PENDING", "SHIPPED", "DELIVERED"))
 *     .build();
 * }</pre>
 *
 * @param enumName the database enum type name (e.g., "order_status")
 * @param columnNames the column name that uses this enum type
 * @param enumValues the {@link List} of allowed {@link String} values (defensively copied)
 * @see Column
 */
public record DbEnum(String enumName, List<String> columnNames, List<String> enumValues) {

  public DbEnum {
    Validators.isNotBlank(enumName, "enumName");
    Validators.isNotNull(columnNames, "columnNames");
    Validators.isNotNull(enumValues, "enumValues");
    Validators.containsNoNullElements(columnNames, "columnNames");
    Validators.containsNoNullElements(enumValues, "enumValues");
    columnNames = List.copyOf(columnNames);
    enumValues = List.copyOf(enumValues);
  }

  /**
   * Creates a new builder for constructing DbEnum instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link DbEnum} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * DbEnum roleEnum = DbEnum.builder()
   *     .enumName("user_role")
   *     .columnName("role")
   *     .enumValues(List.of("ADMIN", "USER", "GUEST"))
   *     .build();
   * }</pre>
   *
   * @see DbEnum
   */
  public static final class Builder {
    private String enumName;
    private List<String> columnNames;
    private List<String> enumValues;

    /**
     * Sets the database enum type name.
     *
     * @param enumName the enum type name (e.g., "order_status")
     * @return this builder instance for method chaining
     */
    public Builder enumName(final String enumName) {
      this.enumName = enumName;
      return this;
    }

    /**
     * Sets the column names that use this enum type.
     *
     * @param columnNames the {@link List} of column names (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder columnNames(final List<String> columnNames) {
      Validators.isNotNull(columnNames, "columnNames");
      Validators.containsNoNullElements(columnNames, "columnNames");
      this.columnNames = List.copyOf(columnNames);
      return this;
    }

    /**
     * Sets the allowed enum values.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param enumValues the {@link List} of {@link String} values (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder enumValues(final List<String> enumValues) {
      Validators.isNotNull(enumValues, "enumValues");
      Validators.containsNoNullElements(enumValues, "enumValues");

      this.enumValues = List.copyOf(enumValues);
      return this;
    }

    /**
     * Builds and returns a new {@link DbEnum} instance.
     *
     * @return a new immutable {@link DbEnum} instance
     * @throws ValidationException if any required field is null
     */
    public DbEnum build() {
      return new DbEnum(enumName, columnNames, enumValues);
    }
  }
}
