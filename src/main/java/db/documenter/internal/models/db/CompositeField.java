package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;

/**
 * Represents a single field within a database composite type.
 *
 * <p>Composite types (created via {@code CREATE TYPE ... AS (...)}) consist of multiple fields,
 * each with a name, data type, and ordinal position. This record captures the metadata for one such
 * field.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> Field name and type must not be blank. Position must be positive to reflect
 * the 1-based ordinal position in the composite type definition.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // PostgreSQL: CREATE TYPE address AS (street varchar(200), city varchar(100), zip varchar(10));
 * CompositeField streetField = CompositeField.builder()
 *     .fieldName("street")
 *     .fieldType("character varying(200)")
 *     .position(1)
 *     .build();
 * }</pre>
 *
 * @param fieldName the field identifier within the composite type (e.g., "street", "city")
 * @param fieldType the SQL data type for this field (e.g., "character varying(200)", "integer")
 * @param position the 1-based ordinal position of this field in the composite type definition
 * @see DbCompositeType
 */
public record CompositeField(String fieldName, String fieldType, int position) {

  public CompositeField {
    Validators.isNotBlank(fieldName, "fieldName");
    Validators.isNotBlank(fieldType, "fieldType");
    Validators.isPositive(position, "position");
  }

  /**
   * Creates a new builder for constructing CompositeField instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link CompositeField} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * CompositeField field = CompositeField.builder()
   *     .fieldName("amount")
   *     .fieldType("numeric(10,2)")
   *     .position(1)
   *     .build();
   * }</pre>
   *
   * @see CompositeField
   */
  public static final class Builder {
    private String fieldName;
    private String fieldType;
    private int position;

    /**
     * Sets the field name.
     *
     * @param fieldName the field identifier (e.g., "street", "postal_code")
     * @return this builder instance for method chaining
     */
    public Builder fieldName(final String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    /**
     * Sets the SQL data type for this field.
     *
     * <p><b>Examples:</b> "character varying(200)", "integer", "numeric(10,2)", "timestamp"
     *
     * @param fieldType the SQL data type
     * @return this builder instance for method chaining
     */
    public Builder fieldType(final String fieldType) {
      this.fieldType = fieldType;
      return this;
    }

    /**
     * Sets the ordinal position of this field within the composite type.
     *
     * <p><b>Note:</b> PostgreSQL uses 1-based positioning for composite type fields.
     *
     * @param position the 1-based position (must be positive)
     * @return this builder instance for method chaining
     */
    public Builder position(final int position) {
      this.position = position;
      return this;
    }

    /**
     * Builds and returns a new {@link CompositeField} instance.
     *
     * @return a new immutable {@link CompositeField} instance
     * @throws ValidationException if any required field is null, blank, or invalid
     */
    public CompositeField build() {
      return new CompositeField(fieldName, fieldType, position);
    }
  }
}
