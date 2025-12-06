/**
 * Domain models representing database metadata for PlantUML diagram generation.
 *
 * <p>This package contains immutable record-based models that capture the structure and
 * relationships of database objects. These models form the core data layer of the application,
 * providing a clean abstraction over database metadata.
 *
 * <h2>Core Models</h2>
 *
 * <ul>
 *   <li>{@link db.documenter.internal.models.db.Schema} - Top-level container grouping tables and
 *       enums within a database schema
 *   <li>{@link db.documenter.internal.models.db.Table} - Represents a database table with columns,
 *       primary key, and foreign keys
 *   <li>{@link db.documenter.internal.models.db.Column} - Represents a column with data type and
 *       constraints
 *   <li>{@link db.documenter.internal.models.db.PrimaryKey} - Primary key constraint (single or
 *       composite)
 *   <li>{@link db.documenter.internal.models.db.ForeignKey} - Foreign key relationship between
 *       tables
 *   <li>{@link db.documenter.internal.models.db.DbEnum} - Custom enum type definition (e.g.,
 *       PostgreSQL CREATE TYPE)
 * </ul>
 *
 * <h2>Supporting Types</h2>
 *
 * <ul>
 *   <li>{@link db.documenter.internal.models.db.Constraint} - Column-level constraints (FK, UNIQUE,
 *       AUTO_INCREMENT, etc.)
 * </ul>
 *
 * <h2>Design Principles</h2>
 *
 * <p><b>Immutability:</b> All models are immutable records with defensive copying of mutable
 * collections. This ensures thread-safety and prevents accidental modification.
 *
 * <p><b>Builder Pattern:</b> All models provide fluent builder APIs for convenient construction.
 * Builders do not provide default values - all required fields must be explicitly set.
 *
 * <p><b>Separation of Concerns:</b> Models are data-only and have no knowledge of rendering or
 * formatting. They represent the "what" (database metadata) not the "how" (PlantUML generation).
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Build a column with constraints
 * Column emailColumn = Column.builder()
 *     .name("email")
 *     .dataType("varchar")
 *     .maximumLength(255)
 *     .constraints(List.of(Constraint.UNIQUE, Constraint.NULLABLE))
 *     .build();
 *
 * // Build a foreign key
 * ForeignKey userFk = ForeignKey.builder()
 *     .name("fk_orders_user")
 *     .sourceTable("orders")
 *     .sourceColumn("user_id")
 *     .targetTable("users")
 *     .targetColumn("id")
 *     .referencedSchema("public")
 *     .isNullable(false)
 *     .build();
 *
 * // Build a table
 * Table ordersTable = Table.builder()
 *     .name("orders")
 *     .columns(List.of(idColumn, userIdColumn, emailColumn))
 *     .primaryKey(primaryKey)
 *     .foreignKeys(List.of(userFk))
 *     .build();
 *
 * // Build a schema
 * Schema publicSchema = Schema.builder()
 *     .name("public")
 *     .tables(List.of(ordersTable, usersTable))
 *     .dbEnums(List.of(orderStatusEnum))
 *     .build();
 * }</pre>
 *
 * @see db.documenter.internal.builder
 * @see db.documenter.internal.mapper
 * @see db.documenter.internal.validation.Validators
 */
package db.documenter.internal.models.db;
