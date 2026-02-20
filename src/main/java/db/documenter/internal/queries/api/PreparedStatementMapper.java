package db.documenter.internal.queries.api;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Contract for binding parameters to JDBC {@link PreparedStatement} instances.
 *
 * <p>Implementations set the correct parameter values on each prepared statement before it is
 * executed. The parameter binding is database-engine-specific (e.g., PostgreSQL uses {@code
 * information_schema} column names that differ from MySQL).
 *
 * @see
 *     db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper
 */
public interface PreparedStatementMapper {

  void prepareTableInfoStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  void prepareColumnInfoStatement(
      PreparedStatement preparedStatement, String schema, String tableName) throws SQLException;

  /**
   * Binds parameters for the primary key info query.
   *
   * <p>This method is only called for tables. Views and materialized views cannot have primary keys
   * in PostgreSQL.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @param tableName the table name
   * @throws SQLException if a database access error occurs
   */
  void preparePrimaryKeyInfoStatement(
      PreparedStatement preparedStatement, String schema, String tableName) throws SQLException;

  /**
   * Binds parameters for the foreign key info query.
   *
   * <p>This method is only called for tables. Views and materialized views cannot have foreign key
   * constraints in PostgreSQL.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @param tableName the table name
   * @throws SQLException if a database access error occurs
   */
  void prepareForeignKeyInfoStatement(
      PreparedStatement preparedStatement, String schema, String tableName) throws SQLException;

  void prepareEnumInfoStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  void prepareEnumValuesStatement(
      PreparedStatement preparedStatement, String schema, String enumName) throws SQLException;

  void prepareColumnUdtMappingsStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  void prepareCompositeTypeInfoStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  /**
   * Binds parameters for the view info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  void prepareViewInfoStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  /**
   * Binds parameters for the materialized view info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  void prepareMaterializedViewInfoStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  /**
   * Binds parameters for the partition children query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  void preparePartitionChildrenStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;
}
