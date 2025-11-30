package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.NonNull;

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
 * @param columnName the column name that uses this enum type
 * @param enumValues the {@link List} of allowed {@link String} values (defensively copied)
 * @see Column
 */
public record DbEnum(
    @NonNull String enumName, @NonNull String columnName, @NonNull List<String> enumValues) {

  public DbEnum {
    Validators.isNotNull(enumName, "enumName");
    Validators.isNotNull(columnName, "columnName");
    Validators.isNotNull(enumValues, "enumValues");
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
    private String columnName;
    private List<String> enumValues;

    /**
     * Sets the database enum type name.
     *
     * @param enumName the enum type name (e.g., "order_status")
     * @return this builder instance for method chaining
     */
    public Builder enumName(final @NonNull String enumName) {
      this.enumName = enumName;
      return this;
    }

    /**
     * Sets the column name that uses this enum type.
     *
     * @param columnName the column name
     * @return this builder instance for method chaining
     */
    public Builder columnName(final @NonNull String columnName) {
      this.columnName = columnName;
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
    public Builder enumValues(final @NonNull List<String> enumValues) {
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
      return new DbEnum(enumName, columnName, enumValues);
    }
  }
}
