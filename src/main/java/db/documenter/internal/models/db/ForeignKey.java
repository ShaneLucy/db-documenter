package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;

/**
 * Represents a foreign key relationship between database tables.
 *
 * <p>This record captures the metadata of a foreign key constraint, including source and target
 * table/column information, the referenced schema, nullability, and the referential actions applied
 * on delete and update.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> All string parameters are validated as non-blank, and both referential
 * action parameters are validated as non-null, in the compact constructor.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // orders.user_id -> users.id (nullable, cascade delete)
 * ForeignKey fk = ForeignKey.builder()
 *     .name("fk_orders_user")
 *     .sourceTable("orders")
 *     .sourceColumn("user_id")
 *     .targetTable("users")
 *     .targetColumn("id")
 *     .referencedSchema("public")
 *     .isNullable(true)
 *     .onDeleteAction(ReferentialAction.CASCADE)
 *     .onUpdateAction(ReferentialAction.NO_ACTION)
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
 * @param onDeleteAction the referential action applied to dependent rows when the referenced row is
 *     deleted; defaults to {@link ReferentialAction#NO_ACTION} if not set via the builder
 * @param onUpdateAction the referential action applied to dependent rows when the referenced row is
 *     updated; defaults to {@link ReferentialAction#NO_ACTION} if not set via the builder
 * @see Table
 * @see ReferentialAction
 */
public record ForeignKey(
    String name,
    String sourceTable,
    String sourceColumn,
    String targetTable,
    String targetColumn,
    String referencedSchema,
    boolean isNullable,
    ReferentialAction onDeleteAction,
    ReferentialAction onUpdateAction) {

  public ForeignKey {
    Validators.isNotBlank(name, "name");
    Validators.isNotBlank(sourceTable, "sourceTable");
    Validators.isNotBlank(sourceColumn, "sourceColumn");
    Validators.isNotBlank(targetTable, "targetTable");
    Validators.isNotBlank(targetColumn, "targetColumn");
    Validators.isNotBlank(referencedSchema, "referencedSchema");
    Validators.isNotNull(onDeleteAction, "onDeleteAction");
    Validators.isNotNull(onUpdateAction, "onUpdateAction");
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
   * <p>Both {@code onDeleteAction} and {@code onUpdateAction} default to {@link
   * ReferentialAction#NO_ACTION} when not explicitly set, ensuring backwards-compatible
   * construction from code that predates referential action support.
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
   *     .onDeleteAction(ReferentialAction.CASCADE)
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
    private ReferentialAction onDeleteAction = ReferentialAction.NO_ACTION;
    private ReferentialAction onUpdateAction = ReferentialAction.NO_ACTION;

    /**
     * @return this builder for method chaining
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * @return this builder for method chaining
     */
    public Builder sourceTable(final String sourceTable) {
      this.sourceTable = sourceTable;
      return this;
    }

    /**
     * @return this builder for method chaining
     */
    public Builder sourceColumn(final String sourceColumn) {
      this.sourceColumn = sourceColumn;
      return this;
    }

    /**
     * @return this builder for method chaining
     */
    public Builder targetTable(final String targetTable) {
      this.targetTable = targetTable;
      return this;
    }

    /**
     * @return this builder for method chaining
     */
    public Builder targetColumn(final String targetColumn) {
      this.targetColumn = targetColumn;
      return this;
    }

    /**
     * @return this builder for method chaining
     */
    public Builder referencedSchema(final String referencedSchema) {
      this.referencedSchema = referencedSchema;
      return this;
    }

    /**
     * Sets whether the foreign key column allows NULL values.
     *
     * @param isNullable true if the column is nullable, false otherwise
     * @return this builder for method chaining
     */
    public Builder isNullable(final boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    /**
     * Sets the referential action applied when the referenced row is deleted.
     *
     * @param onDeleteAction must not be null; defaults to {@link ReferentialAction#NO_ACTION}
     * @return this builder for method chaining
     */
    public Builder onDeleteAction(final ReferentialAction onDeleteAction) {
      this.onDeleteAction = onDeleteAction;
      return this;
    }

    /**
     * Sets the referential action applied when the referenced row is updated.
     *
     * @param onUpdateAction must not be null; defaults to {@link ReferentialAction#NO_ACTION}
     * @return this builder for method chaining
     */
    public Builder onUpdateAction(final ReferentialAction onUpdateAction) {
      this.onUpdateAction = onUpdateAction;
      return this;
    }

    /**
     * Builds and returns a new {@link ForeignKey} instance.
     *
     * @return a new immutable {@link ForeignKey} instance
     * @throws ValidationException if any required string field is blank or either referential
     *     action field is null
     */
    public ForeignKey build() {
      return new ForeignKey(
          name,
          sourceTable,
          sourceColumn,
          targetTable,
          targetColumn,
          referencedSchema,
          isNullable,
          onDeleteAction,
          onUpdateAction);
    }
  }
}
