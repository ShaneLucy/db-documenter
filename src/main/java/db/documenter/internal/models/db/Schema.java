package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Represents a database schema containing tables and custom enum types.
 *
 * <p>This record is the top-level container for database metadata, grouping related tables and enum
 * types within a named schema. In PostgreSQL, a schema is a namespace within a database.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. Both the tables and dbEnums
 * lists are defensively copied to prevent external modification.
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
public record Schema(
    @NonNull String name, @NonNull List<Table> tables, @NonNull List<DbEnum> dbEnums) {

  public Schema {
    Validators.isNotNull(name, "name");
    Validators.isNotNull(tables, "tables");
    Validators.isNotNull(dbEnums, "dbEnums");
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
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
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
  public static class Builder {
    private String name;
    private List<Table> tables;
    private List<DbEnum> dbEnums;

    /**
     * Sets the schema name.
     *
     * @param name the schema name
     * @return this builder instance for method chaining
     */
    public Builder name(final @NonNull String name) {
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
    public Builder tables(final @NonNull List<Table> tables) {
      this.tables = List.copyOf(tables);
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
    public Builder dbEnums(final @NonNull List<DbEnum> dbEnums) {
      this.dbEnums = List.copyOf(dbEnums);
      return this;
    }

    /**
     * Builds and returns a new {@link Schema} instance.
     *
     * @return a new immutable {@link Schema} instance
     * @throws ValidationException if any required field is null
     */
    public Schema build() {
      return new Schema(name, tables, dbEnums);
    }
  }
}
