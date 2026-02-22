package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.View;
import java.sql.SQLException;
import java.util.List;

/**
 * Contract for querying the common database metadata required to build schema documentation.
 *
 * <p>Defines the five operations shared across all supported database engines: tables, columns,
 * primary keys, foreign keys, and views. Engine-specific operations (e.g., enums, composite types,
 * materialized views, partitions) are declared on concrete subclasses and are not part of this
 * interface.
 *
 * <p><b>Design Note:</b> Methods that accept a {@code tableName} string rather than a {@link Table}
 * object are intentional — the query layer should not depend on partially-constructed domain
 * objects. Table and view names are plain strings at query time.
 *
 * @see db.documenter.internal.queries.AbstractQueryRunner
 * @see db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner
 */
public interface QueryRunner {

  /**
   * Retrieves all base tables in the specified schema.
   *
   * @param schema the schema to query for tables
   * @return immutable list of tables (name-only, columns empty); never null, may be empty
   * @throws SQLException if the database query fails
   */
  List<Table> getTableInfo(String schema) throws SQLException;

  /**
   * Retrieves all columns for a named table or regular view within the given schema.
   *
   * @param schema the schema containing the table or view
   * @param tableName the name of the table or regular view to query
   * @return immutable list of columns; never null, may be empty
   * @throws SQLException if the database query fails
   */
  List<Column> getColumnInfo(String schema, String tableName) throws SQLException;

  /**
   * Retrieves the primary key constraint for a named table.
   *
   * <p>Views and materialized views cannot have primary key constraints and should not be passed to
   * this method.
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
   * <p>Views and materialized views cannot have foreign key constraints and should not be passed to
   * this method.
   *
   * @param schema the schema containing the table
   * @param tableName the name of the table to query
   * @return immutable list of foreign keys; never null, may be empty
   * @throws SQLException if the database query fails
   */
  List<ForeignKey> getForeignKeyInfo(String schema, String tableName) throws SQLException;

  /**
   * Retrieves all views defined in the specified schema.
   *
   * <p>Returns stub {@link View} objects with name only and an empty column list. Columns are
   * populated separately by the builder using {@link #getColumnInfo(String, String)}, following the
   * same two-phase pattern used for tables.
   *
   * @param schema the schema to query for views
   * @return immutable list of views (name-only, columns empty); never null, may be empty
   * @throws SQLException if the database query fails
   */
  List<View> getViewInfo(String schema) throws SQLException;
}
