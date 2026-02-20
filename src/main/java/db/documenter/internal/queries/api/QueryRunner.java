package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.View;
import db.documenter.internal.models.db.postgresql.UdtReference;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Contract for querying database metadata required to build schema documentation.
 *
 * <p>Implementations retrieve structural information (tables, views, columns, constraints,
 * user-defined types) from a specific database engine. Callers receive domain model objects; the
 * raw JDBC mapping is an implementation detail hidden behind this interface.
 *
 * <p><b>Design Note:</b> Methods that accept a {@code tableName} string rather than a {@link Table}
 * object are intentional — the query layer should not depend on partially-constructed domain
 * objects. Table, view, and materialized view names are plain strings at query time.
 *
 * @see db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner
 */
public interface QueryRunner {

  List<Table> getTableInfo(String schema) throws SQLException;

  /**
   * Retrieves all columns for a named table or view within the given schema.
   *
   * <p>This method is reused for tables, views, and materialized views because all three object
   * types expose their column metadata through {@code information_schema.columns}.
   *
   * @param schema the schema containing the table or view
   * @param tableName the name of the table, view, or materialized view to query
   * @return immutable list of columns; never null, may be empty
   * @throws SQLException if the database query fails
   */
  List<Column> getColumnInfo(String schema, String tableName) throws SQLException;

  /**
   * Retrieves the primary key constraint for a named table.
   *
   * <p>This method is only meaningful for tables. Views and materialized views in PostgreSQL cannot
   * have primary key constraints, so this method should not be called for those object types.
   *
   * @param schema the schema containing the table
   * @param tableName the name of the table to query
   * @return the {@link PrimaryKey} or null if the table has no primary key
   * @throws SQLException if the database query fails
   */
  PrimaryKey getPrimaryKeyInfo(String schema, String tableName) throws SQLException;

  /**
   * Retrieves all foreign key relationships originating from a named table.
   *
   * <p>This method is only meaningful for tables. Views and materialized views in PostgreSQL cannot
   * have foreign key constraints, so this method should not be called for those object types.
   *
   * @param schema the schema containing the table
   * @param tableName the name of the table to query
   * @return immutable list of foreign keys; never null, may be empty
   * @throws SQLException if the database query fails
   */
  List<ForeignKey> getForeignKeyInfo(String schema, String tableName) throws SQLException;

  List<DbEnum> getEnumInfo(String schema) throws SQLException;

  List<String> getEnumValues(String schema, DbEnum dbEnums) throws SQLException;

  /**
   * Retrieves mappings of columns to their user-defined types (enums/composite types).
   *
   * <p>Used to resolve which columns reference which types across schemas. This enables proper
   * cross-schema type resolution where a column in one schema may reference a UDT defined in
   * another schema.
   *
   * <p><b>Cross-Schema Resolution:</b> The returned map allows looking up the defining schema and
   * type name for any column that uses a user-defined type, even when the type is defined in a
   * different schema than the table.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * // Get UDT mappings for 'core' schema
   * Map<ColumnKey, UdtReference> mappings = queryRunner.getColumnUdtMappings("core");
   *
   * // Look up where the 'status' column's type is defined
   * ColumnKey key = new ColumnKey("users", "status");
   * UdtReference udtRef = mappings.get(key);
   * // udtRef might be UdtReference("auth", "account_status")
   * // meaning the type is defined in 'auth' schema, not 'core'
   * }</pre>
   *
   * @param schemaName the schema containing the tables to query
   * @return immutable map of column keys to UDT references; never null, may be empty if no UDTs are
   *     used
   * @throws SQLException if database query fails
   */
  Map<ColumnKey, UdtReference> getColumnUdtMappings(String schemaName) throws SQLException;

  /**
   * Retrieves all composite types defined in the specified schema.
   *
   * <p>Composite types are user-defined types created with {@code CREATE TYPE ... AS (...)} that
   * define structured types with multiple named fields. These are distinct from tables in that they
   * are type definitions, not data containers.
   *
   * <p><b>PlantUML Rendering:</b> Composite types are rendered with the {@code <<composite>>}
   * stereotype to visually distinguish them from tables and enums.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * // PostgreSQL: CREATE TYPE core.address AS (
   * //   street varchar(200),
   * //   city varchar(100),
   * //   postal_code varchar(10)
   * // );
   * List<DbCompositeType> compositeTypes = queryRunner.getCompositeTypeInfo("core");
   * // Returns list containing the 'address' composite type with its 3 fields
   * }</pre>
   *
   * @param schema the schema to query for composite types
   * @return immutable list of composite types; never null, may be empty if no composite types exist
   * @throws SQLException if database query fails
   */
  List<DbCompositeType> getCompositeTypeInfo(String schema) throws SQLException;

  /**
   * Retrieves all views defined in the specified schema.
   *
   * <p>Returns stub {@link View} objects with name only and an empty column list. Columns are
   * populated separately by the builder using {@link #getColumnInfo(String, String)}, following the
   * same two-phase pattern used for tables.
   *
   * @param schema the schema to query for views
   * @return immutable list of views (name-only, columns empty); never null, may be empty
   * @throws SQLException if database query fails
   */
  List<View> getViewInfo(String schema) throws SQLException;

  /**
   * Retrieves all materialized views defined in the specified schema.
   *
   * <p>Returns stub {@link MaterializedView} objects with name only and an empty column list.
   * Columns are populated separately by the builder using {@link #getColumnInfo(String, String)},
   * following the same two-phase pattern used for tables.
   *
   * @param schema the schema to query for materialized views
   * @return immutable list of materialized views (name-only, columns empty); never null, may be
   *     empty
   * @throws SQLException if database query fails
   */
  List<MaterializedView> getMaterializedViewInfo(String schema) throws SQLException;

  /**
   * Retrieves the child partition names grouped by parent table name for all partitioned tables in
   * the specified schema.
   *
   * <p>Returns a map from parent table name to an ordered list of child partition names. Only
   * tables with {@code relkind = 'p'} (partitioned tables) appear as keys; regular tables are
   * absent from the result.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * // PostgreSQL: CREATE TABLE audit_log (...) PARTITION BY RANGE (event_time);
   * Map<String, List<String>> children = queryRunner.getPartitionChildren("audit");
   * // Returns {"audit_log" -> ["audit_log_2024_11", "audit_log_2024_12", "audit_log_default"]}
   * }</pre>
   *
   * @param schema the schema to query for partitioned tables
   * @return immutable map of parent table name to ordered child partition names; never null, may be
   *     empty
   * @throws SQLException if database query fails
   */
  Map<String, List<String>> getPartitionChildren(String schema) throws SQLException;
}
