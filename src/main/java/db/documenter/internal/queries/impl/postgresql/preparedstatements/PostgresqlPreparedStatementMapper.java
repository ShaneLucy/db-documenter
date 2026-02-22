package db.documenter.internal.queries.impl.postgresql.preparedstatements;

import db.documenter.internal.queries.AbstractPreparedStatementMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PostgreSQL-specific implementation of {@link
 * db.documenter.internal.queries.api.PreparedStatementMapper}.
 *
 * <p>Provides parameter binding for the seven PostgreSQL-specific metadata queries (enums, enum
 * values, column UDT mappings, composite types, views, materialized views, materialized view
 * columns, and partition children). The five generic bindings (table info, column info, primary key
 * info, foreign key info, and view info) are inherited from {@link AbstractPreparedStatementMapper}
 * and follow the convention: schema is always parameter 1; the object name (table, view, enum name)
 * is parameter 2 when present.
 */
public final class PostgresqlPreparedStatementMapper extends AbstractPreparedStatementMapper {

  /**
   * Binds parameters for the enum info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  public void prepareEnumInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  /**
   * Binds parameters for the enum values query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @param enumName the name of the enum type
   * @throws SQLException if a database access error occurs
   */
  public void prepareEnumValuesStatement(
      final PreparedStatement preparedStatement, final String schema, final String enumName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, enumName);
  }

  /**
   * Binds parameters for the column UDT mappings query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  public void prepareColumnUdtMappingsStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  /**
   * Binds parameters for the composite type info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  public void prepareCompositeTypeInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  /**
   * Binds parameters for the materialized view info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  public void prepareMaterializedViewInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  /**
   * Binds parameters for the materialized view column info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @param matViewName the name of the materialized view
   * @throws SQLException if a database access error occurs
   */
  public void prepareMaterializedViewColumnInfoStatement(
      final PreparedStatement preparedStatement, final String schema, final String matViewName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, matViewName);
  }

  /**
   * Binds parameters for the partition children query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  public void preparePartitionChildrenStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }
}
