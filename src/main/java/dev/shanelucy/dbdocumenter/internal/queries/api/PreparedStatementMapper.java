package db.documenter.internal.queries.api;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Contract for binding parameters to JDBC {@link PreparedStatement} instances.
 *
 * <p>Defines the four parameter-binding operations shared across all supported database engines,
 * corresponding to the four methods in {@link QueryRunner}. Engine-specific operations (e.g.,
 * PostgreSQL enum queries, UDT mappings) are declared on concrete subclasses and are not part of
 * this interface.
 *
 * <p>Implementations set the correct parameter values on each prepared statement before it is
 * executed. The parameter binding is database-engine-specific (e.g., PostgreSQL uses {@code
 * information_schema} column names that differ from MySQL).
 *
 * @see db.documenter.internal.queries.AbstractPreparedStatementMapper
 * @see
 *     db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper
 */
public interface PreparedStatementMapper {

  /**
   * Binds parameters for the table info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  void prepareTableInfoStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  /**
   * Binds parameters for the column info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @param tableName the table name
   * @throws SQLException if a database access error occurs
   */
  void prepareColumnInfoStatement(
      PreparedStatement preparedStatement, String schema, String tableName) throws SQLException;

  /**
   * Binds parameters for the primary key info query.
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
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @param tableName the table name
   * @throws SQLException if a database access error occurs
   */
  void prepareForeignKeyInfoStatement(
      PreparedStatement preparedStatement, String schema, String tableName) throws SQLException;
}
