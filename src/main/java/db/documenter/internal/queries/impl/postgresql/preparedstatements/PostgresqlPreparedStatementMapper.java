package db.documenter.internal.queries.impl.postgresql.preparedstatements;

import db.documenter.internal.queries.api.PreparedStatementMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PostgreSQL-specific implementation of {@link PreparedStatementMapper}.
 *
 * <p>Binds schema and object-name parameters for each PostgreSQL metadata query. All methods follow
 * the same convention: schema is always parameter 1; the object name (table, view, enum name) is
 * parameter 2 when present.
 */
public final class PostgresqlPreparedStatementMapper implements PreparedStatementMapper {

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
  public void prepareEnumInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  @Override
  public void prepareEnumValuesStatement(
      final PreparedStatement preparedStatement, final String schema, final String enumName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, enumName);
  }

  @Override
  public void prepareColumnUdtMappingsStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  @Override
  public void prepareCompositeTypeInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  @Override
  public void prepareViewInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  @Override
  public void prepareMaterializedViewInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  @Override
  public void prepareMaterializedViewColumnInfoStatement(
      final PreparedStatement preparedStatement, final String schema, final String matViewName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, matViewName);
  }

  @Override
  public void preparePartitionChildrenStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }
}
