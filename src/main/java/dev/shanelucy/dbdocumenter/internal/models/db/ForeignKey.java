package dev.shanelucy.dbdocumenter.internal.models.db;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import org.jspecify.annotations.Nullable;

/**
 * Represents a foreign key relationship between database tables.
 *
 * <p>Captures the structural metadata needed to render an ERD relationship line: which tables and
 * columns are involved, which schema owns the referenced table, and whether the FK column is
 * nullable (which drives cardinality notation).
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> All string parameters are validated as non-blank in the compact
 * constructor.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // orders.user_id -> users.id (optional/nullable relationship)
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
 *     for required relationships. Has no builder default so a forgotten call cannot silently change
 *     cardinality
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
    private @Nullable String name;
    private @Nullable String sourceTable;
    private @Nullable String sourceColumn;
    private @Nullable String targetTable;
    private @Nullable String targetColumn;
    private @Nullable String referencedSchema;
    private @Nullable Boolean isNullable;

    /** Returns this builder for method chaining. */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder sourceTable(final String sourceTable) {
      this.sourceTable = sourceTable;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder sourceColumn(final String sourceColumn) {
      this.sourceColumn = sourceColumn;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder targetTable(final String targetTable) {
      this.targetTable = targetTable;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder targetColumn(final String targetColumn) {
      this.targetColumn = targetColumn;
      return this;
    }

    /** Returns this builder for method chaining. */
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
     * Builds and returns a new {@link ForeignKey} instance.
     *
     * @return a new immutable {@link ForeignKey} instance
     * @throws ValidationException if any required string field is blank or {@code isNullable} was
     *     never set
     */
    public ForeignKey build() {
      return new ForeignKey(
          Validators.isNotBlank(name, "name"),
          Validators.isNotBlank(sourceTable, "sourceTable"),
          Validators.isNotBlank(sourceColumn, "sourceColumn"),
          Validators.isNotBlank(targetTable, "targetTable"),
          Validators.isNotBlank(targetColumn, "targetColumn"),
          Validators.isNotBlank(referencedSchema, "referencedSchema"),
          Validators.isNotNull(isNullable, "isNullable"));
    }
  }
}
