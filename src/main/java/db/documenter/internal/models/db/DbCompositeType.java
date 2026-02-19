package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;

/**
 * Represents a database composite type definition.
 *
 * <p>Composite types (also called row types or user-defined types) are created using {@code CREATE
 * TYPE ... AS (...)} and define structured types with multiple named fields. Unlike tables, these
 * are type definitions used in column declarations, function returns, or other type contexts.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. The fields list is defensively
 * copied to prevent external modification.
 *
 * <p><b>Schema Qualification:</b> Composite types are identified by both schema and type name to
 * support multi-schema databases where different schemas may define types with the same name.
 *
 * <p><b>PlantUML Rendering:</b> Composite types are rendered with the {@code <<composite>>}
 * stereotype to distinguish them from tables and enums.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // PostgreSQL: CREATE TYPE core.address AS (
 * //   street_line1 varchar(200),
 * //   city varchar(100),
 * //   postal_code varchar(10)
 * // );
 * DbCompositeType addressType = DbCompositeType.builder()
 *     .typeName("address")
 *     .schemaName("core")
 *     .fields(List.of(
 *         CompositeField.builder()
 *             .fieldName("street_line1")
 *             .fieldType("character varying(200)")
 *             .position(1)
 *             .build(),
 *         CompositeField.builder()
 *             .fieldName("city")
 *             .fieldType("character varying(100)")
 *             .position(2)
 *             .build(),
 *         CompositeField.builder()
 *             .fieldName("postal_code")
 *             .fieldType("character varying(10)")
 *             .position(3)
 *             .build()
 *     ))
 *     .build();
 * }</pre>
 *
 * @param typeName the database composite type name (e.g., "address", "money_amount")
 * @param schemaName the schema containing this composite type (e.g., "core", "public")
 * @param fields the {@link List} of {@link CompositeField} instances defining this type's structure
 *     (defensively copied)
 * @see CompositeField
 */
public record DbCompositeType(String typeName, String schemaName, List<CompositeField> fields) {

  public DbCompositeType {
    Validators.isNotBlank(typeName, "typeName");
    Validators.isNotBlank(schemaName, "schemaName");
    Validators.isNotNull(fields, "fields");
    Validators.containsNoNullElements(fields, "fields");
    fields = List.copyOf(fields);
  }

  /**
   * Creates a new builder for constructing DbCompositeType instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link DbCompositeType} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * DbCompositeType moneyType = DbCompositeType.builder()
   *     .typeName("money_amount")
   *     .schemaName("core")
   *     .fields(List.of(
   *         CompositeField.builder()
   *             .fieldName("amount")
   *             .fieldType("numeric(10,2)")
   *             .position(1)
   *             .build(),
   *         CompositeField.builder()
   *             .fieldName("currency_code")
   *             .fieldType("character(3)")
   *             .position(2)
   *             .build()
   *     ))
   *     .build();
   * }</pre>
   *
   * @see DbCompositeType
   */
  public static final class Builder {
    private String typeName;
    private String schemaName;
    private List<CompositeField> fields;

    /**
     * Sets the composite type name.
     *
     * @param typeName the type name (e.g., "address", "money_amount")
     * @return this builder instance for method chaining
     */
    public Builder typeName(final String typeName) {
      this.typeName = typeName;
      return this;
    }

    /**
     * Sets the schema containing this composite type.
     *
     * @param schemaName the schema name (e.g., "core", "public")
     * @return this builder instance for method chaining
     */
    public Builder schemaName(final String schemaName) {
      this.schemaName = schemaName;
      return this;
    }

    /**
     * Sets the fields that compose this type.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param fields the {@link List} of {@link CompositeField} instances (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder fields(final List<CompositeField> fields) {
      Validators.isNotNull(fields, "fields");
      Validators.containsNoNullElements(fields, "fields");

      this.fields = List.copyOf(fields);
      return this;
    }

    /**
     * Builds and returns a new {@link DbCompositeType} instance.
     *
     * @return a new immutable {@link DbCompositeType} instance
     * @throws ValidationException if any required field is null or blank
     */
    public DbCompositeType build() {
      return new DbCompositeType(typeName, schemaName, fields);
    }
  }
}
