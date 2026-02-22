package db.documenter.internal.queries;

import db.documenter.internal.queries.api.PreparedStatementMapper;
import db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Sealed base class for all database-specific prepared statement mapper implementations.
 *
 * <p>Provides concrete implementations for the five generic parameter-binding methods in the {@link
 * PreparedStatementMapper} interface. All five methods use positional {@link
 * PreparedStatement#setString} calls following the convention that schema is always parameter 1 and
 * the object name (table, view name) is parameter 2 when present. This convention is compatible
 * with the ANSI {@code information_schema} queries implemented in {@link
 * SqlAgnosticPreparedStatements}.
 *
 * <p>The {@code sealed} modifier restricts the class hierarchy to known implementations, enabling
 * exhaustive pattern matching and preventing unexpected extension.
 *
 * <p>Engine-specific parameter binding operations (e.g., PostgreSQL enum queries, composite type
 * queries, materialized view queries) are declared directly on the permitted concrete subclass
 * rather than on this abstract class or the interface.
 *
 * @see PreparedStatementMapper
 * @see PostgresqlPreparedStatementMapper
 */
public abstract sealed class AbstractPreparedStatementMapper implements PreparedStatementMapper
    permits PostgresqlPreparedStatementMapper {

  @Override
  public void prepareTableInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  @Override
  public void prepareColumnInfoStatement(
      final PreparedStatement preparedStatement, final String schema, final String tableName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, tableName);
  }

  @Override
  public void preparePrimaryKeyInfoStatement(
      final PreparedStatement preparedStatement, final String schema, final String tableName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, tableName);
  }

  @Override
  public void prepareForeignKeyInfoStatement(
      final PreparedStatement preparedStatement, final String schema, final String tableName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, tableName);
  }

  @Override
  public void prepareViewInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }
}
