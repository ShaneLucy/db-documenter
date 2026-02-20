package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;

/**
 * Represents a database schema containing tables, views, materialized views, custom enum types, and
 * composite types.
 *
 * <p>This record is the top-level container for database metadata, grouping related database
 * objects within a named schema. In PostgreSQL, a schema is a namespace within a database.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. All lists (tables, views,
 * materializedViews, dbEnums, compositeTypes) are defensively copied to prevent external
 * modification.
 *
 * <p><b>Validation:</b> All parameters are validated as non-null in the compact constructor.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * Schema publicSchema = Schema.builder()
 *     .name("public")
 *     .tables(List.of(usersTable, ordersTable))
 *     .views(List.of(activeUsersView))
 *     .materializedViews(List.of(monthlySummaryMatView))
 *     .dbEnums(List.of(orderStatusEnum))
 *     .compositeTypes(List.of(addressType, moneyType))
 *     .build();
 * }</pre>
 *
 * @param name the schema name (e.g., "public", "inventory")
 * @param tables the {@link List} of {@link Table} instances in this schema (defensively copied)
 * @param views the {@link List} of {@link View} instances in this schema (defensively copied)
 * @param materializedViews the {@link List} of {@link MaterializedView} instances in this schema
 *     (defensively copied)
 * @param dbEnums the {@link List} of {@link DbEnum} instances in this schema (defensively copied)
 * @param compositeTypes the {@link List} of {@link DbCompositeType} instances in this schema
 *     (defensively copied)
 * @see Table
 * @see View
 * @see MaterializedView
 * @see DbEnum
 * @see DbCompositeType
 */
public record Schema(
    String name,
    List<Table> tables,
    List<View> views,
    List<MaterializedView> materializedViews,
    List<DbEnum> dbEnums,
    List<DbCompositeType> compositeTypes) {

  public Schema {
    Validators.isNotBlank(name, "name");
    Validators.isNotNull(tables, "tables");
    Validators.isNotNull(views, "views");
    Validators.isNotNull(materializedViews, "materializedViews");
    Validators.isNotNull(dbEnums, "dbEnums");
    Validators.isNotNull(compositeTypes, "compositeTypes");
    tables = List.copyOf(tables);
    views = List.copyOf(views);
    materializedViews = List.copyOf(materializedViews);
    dbEnums = List.copyOf(dbEnums);
    compositeTypes = List.copyOf(compositeTypes);
  }

  /**
   * Creates a new builder for constructing Schema instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link Schema} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Schema schema = Schema.builder()
   *     .name("inventory")
   *     .tables(List.of(productsTable, categoriesTable))
   *     .views(List.of())
   *     .materializedViews(List.of())
   *     .dbEnums(List.of())
   *     .compositeTypes(List.of(addressType))
   *     .build();
   * }</pre>
   *
   * @see Schema
   */
  public static final class Builder {
    private String name;
    private List<Table> tables;
    private List<View> views;
    private List<MaterializedView> materializedViews;
    private List<DbEnum> dbEnums;
    private List<DbCompositeType> compositeTypes;

    /**
     * Sets the schema name.
     *
     * @param name the schema name
     * @return this builder instance for method chaining
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the tables in this schema.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param tables the {@link List} of {@link Table} instances (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder tables(final List<Table> tables) {
      this.tables = List.copyOf(tables);
      return this;
    }

    /**
     * Sets the views in this schema.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param views the {@link List} of {@link View} instances (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder views(final List<View> views) {
      this.views = List.copyOf(views);
      return this;
    }

    /**
     * Sets the materialized views in this schema.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param materializedViews the {@link List} of {@link MaterializedView} instances (defensively
     *     copied)
     * @return this builder instance for method chaining
     */
    public Builder materializedViews(final List<MaterializedView> materializedViews) {
      this.materializedViews = List.copyOf(materializedViews);
      return this;
    }

    /**
     * Sets the custom enum types in this schema.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param dbEnums the {@link List} of {@link DbEnum} instances (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder dbEnums(final List<DbEnum> dbEnums) {
      this.dbEnums = List.copyOf(dbEnums);
      return this;
    }

    /**
     * Sets the composite types in this schema.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param compositeTypes the {@link List} of {@link DbCompositeType} instances (defensively
     *     copied)
     * @return this builder instance for method chaining
     */
    public Builder compositeTypes(final List<DbCompositeType> compositeTypes) {
      this.compositeTypes = List.copyOf(compositeTypes);
      return this;
    }

    /**
     * Builds and returns a new {@link Schema} instance.
     *
     * @return a new immutable {@link Schema} instance
     * @throws ValidationException if any required field is null
     */
    public Schema build() {
      return new Schema(name, tables, views, materializedViews, dbEnums, compositeTypes);
    }
  }
}
