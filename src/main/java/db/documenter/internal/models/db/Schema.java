package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Represents a database schema containing tables and custom enum types.
 *
 * <p>This record is the top-level container for database metadata, grouping related database
 * objects within a named schema. In PostgreSQL, a schema is a namespace within a database.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. All lists (tables, dbEnums) are
 * defensively copied to prevent external modification.
 *
 * <p><b>Validation:</b> All parameters are validated as non-null in the compact constructor.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * Schema publicSchema = Schema.builder()
 *     .name("public")
 *     .tables(List.of(usersTable, ordersTable))
 *     .dbEnums(List.of(orderStatusEnum))
 *     .build();
 * }</pre>
 *
 * @param name the schema name (e.g., "public", "inventory")
 * @param tables the {@link List} of {@link Table} instances in this schema (defensively copied)
 * @param dbEnums the {@link List} of {@link DbEnum} instances in this schema (defensively copied)
 * @see Table
 * @see DbEnum
 */
public record Schema(String name, List<Table> tables, List<DbEnum> dbEnums) {

  public Schema {
    Validators.isNotBlank(name, "name");
    Validators.containsNoNullElements(tables, "tables");
    Validators.containsNoNullElements(dbEnums, "dbEnums");
    tables = List.copyOf(tables);
    dbEnums = List.copyOf(dbEnums);
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
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Schema schema = Schema.builder()
   *     .name("inventory")
   *     .tables(List.of(productsTable, categoriesTable))
   *     .dbEnums(List.of())
   *     .build();
   * }</pre>
   *
   * @see Schema
   */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable List<Table> tables;
    private @Nullable List<DbEnum> dbEnums;

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
     * @param tables the {@link List} of {@link Table} instances; copied when the schema is built
     * @return this builder instance for method chaining
     */
    public Builder tables(final List<Table> tables) {
      this.tables = tables;
      return this;
    }

    /**
     * Sets the custom enum types in this schema.
     *
     * @param dbEnums the {@link List} of {@link DbEnum} instances; copied when the schema is built
     * @return this builder instance for method chaining
     */
    public Builder dbEnums(final List<DbEnum> dbEnums) {
      this.dbEnums = dbEnums;
      return this;
    }

    /**
     * Builds and returns a new {@link Schema} instance.
     *
     * @return a new immutable {@link Schema} instance
     * @throws ValidationException if any required field is null
     */
    public Schema build() {
      return new Schema(
          Validators.isNotBlank(name, "name"),
          Validators.isNotNull(tables, "tables"),
          Validators.isNotNull(dbEnums, "dbEnums"));
    }
  }
}
