package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;

/**
 * Represents a foreign key relationship between database tables.
 *
 * <p>This record captures the metadata of a foreign key constraint, including source and target
 * table/column information, the referenced schema, and nullability information.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> All string parameters are validated as non-null in the compact constructor.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // orders.user_id -> users.id (nullable)
 * ForeignKey fk = ForeignKey.builder()
 *     .name("fk_orders_user")
 *     .sourceTable("orders")
 *     .sourceColumn("user_id")
 *     .targetTable("users")
 *     .targetColumn("id")
 *     .referencedSchema("public")
 *     .isNullable(true)
 *     .build();
 * }</pre>
 *
 * @param name the foreign key constraint name
 * @param sourceTable the table containing the foreign key
 * @param sourceColumn the column in the source table
 * @param targetTable the referenced table
 * @param targetColumn the referenced column in the target table
 * @param referencedSchema the schema of the referenced table
 * @param isNullable true if the FK column permits NULL (indicating optional relationship); false
 *     for required relationships. Builder defaults to false if not set
 * @see Table
 */
public record ForeignKey(
    String name,
    String sourceTable,
    String sourceColumn,
    String targetTable,
    String targetColumn,
    String referencedSchema,
    boolean isNullable) {

  public ForeignKey {
    Validators.isNotBlank(name, "name");
    Validators.isNotBlank(sourceTable, "sourceTable");
    Validators.isNotBlank(sourceColumn, "sourceColumn");
    Validators.isNotBlank(targetTable, "targetTable");
    Validators.isNotBlank(targetColumn, "targetColumn");
    Validators.isNotBlank(referencedSchema, "referencedSchema");
  }

  /**
   * Creates a new builder for constructing ForeignKey instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link ForeignKey} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * ForeignKey fk = ForeignKey.builder()
   *     .name("fk_order_items_product")
   *     .sourceTable("order_items")
   *     .sourceColumn("product_id")
   *     .targetTable("products")
   *     .targetColumn("id")
   *     .referencedSchema("public")
   *     .isNullable(false)
   *     .build();
   * }</pre>
   *
   * @see ForeignKey
   */
  public static final class Builder {
    private String name;
    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
    private String referencedSchema;
    private boolean isNullable;

    /**
     * Sets the foreign key constraint name.
     *
     * @param name the constraint name
     * @return this builder instance for method chaining
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the source table (the table containing the foreign key).
     *
     * @param sourceTable the source table name
     * @return this builder instance for method chaining
     */
    public Builder sourceTable(final String sourceTable) {
      this.sourceTable = sourceTable;
      return this;
    }

    /**
     * Sets the source column (the column in the source table).
     *
     * @param sourceColumn the source column name
     * @return this builder instance for method chaining
     */
    public Builder sourceColumn(final String sourceColumn) {
      this.sourceColumn = sourceColumn;
      return this;
    }

    /**
     * Sets the target table (the referenced table).
     *
     * @param targetTable the target table name
     * @return this builder instance for method chaining
     */
    public Builder targetTable(final String targetTable) {
      this.targetTable = targetTable;
      return this;
    }

    /**
     * Sets the target column (the referenced column in the target table).
     *
     * @param targetColumn the target column name
     * @return this builder instance for method chaining
     */
    public Builder targetColumn(final String targetColumn) {
      this.targetColumn = targetColumn;
      return this;
    }

    /**
     * Sets the schema of the referenced table.
     *
     * @param referencedSchema the schema name
     * @return this builder instance for method chaining
     */
    public Builder referencedSchema(final String referencedSchema) {
      this.referencedSchema = referencedSchema;
      return this;
    }

    /**
     * Sets whether the foreign key column allows NULL values.
     *
     * @param isNullable true if the column is nullable, false otherwise
     * @return this builder instance for method chaining
     */
    public Builder isNullable(final boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    /**
     * Builds and returns a new {@link ForeignKey} instance.
     *
     * @return a new immutable {@link ForeignKey} instance
     * @throws ValidationException if any required field is null
     */
    public ForeignKey build() {
      return new ForeignKey(
          name, sourceTable, sourceColumn, targetTable, targetColumn, referencedSchema, isNullable);
    }
  }
}
