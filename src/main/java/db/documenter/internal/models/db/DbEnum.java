package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;

/**
 * Represents a database enum type definition.
 *
 * <p>This record captures custom enum types created in the database (e.g., PostgreSQL {@code CREATE
 * TYPE} enums). These are catalog-level objects that define reusable sets of allowed values.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. The enum values list is
 * defensively copied to prevent external modification.
 *
 * <p><b>Schema Qualification:</b> Enums are identified by both schema and name to support
 * multi-schema databases where different schemas may define enums with the same name but different
 * values.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // PostgreSQL: CREATE TYPE core.order_status AS ENUM ('PENDING', 'SHIPPED', 'DELIVERED');
 * DbEnum orderStatus = DbEnum.builder()
 *     .schemaName("core")
 *     .enumName("order_status")
 *     .enumValues(List.of("PENDING", "SHIPPED", "DELIVERED"))
 *     .build();
 * }</pre>
 *
 * @param schemaName the schema containing this enum type (e.g., "core", "public")
 * @param enumName the database enum type name (e.g., "order_status")
 * @param enumValues the {@link List} of allowed {@link String} values (defensively copied)
 */
public record DbEnum(String schemaName, String enumName, List<String> enumValues) {

  public DbEnum {
    Validators.isNotBlank(schemaName, "schemaName");
    Validators.isNotBlank(enumName, "enumName");
    Validators.isNotNull(enumValues, "enumValues");
    Validators.containsNoNullElements(enumValues, "enumValues");
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
   *     .schemaName("core")
   *     .enumName("user_role")
   *     .enumValues(List.of("ADMIN", "USER", "GUEST"))
   *     .build();
   * }</pre>
   *
   * @see DbEnum
   */
  public static final class Builder {
    private String schemaName;
    private String enumName;
    private List<String> enumValues;

    /**
     * Sets the schema containing this enum type.
     *
     * @param schemaName the schema name (e.g., "core", "public")
     * @return this builder instance for method chaining
     */
    public Builder schemaName(final String schemaName) {
      this.schemaName = schemaName;
      return this;
    }

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
     * @throws ValidationException if any required field is null or blank
     */
    public DbEnum build() {
      return new DbEnum(schemaName, enumName, enumValues);
    }
  }
}
