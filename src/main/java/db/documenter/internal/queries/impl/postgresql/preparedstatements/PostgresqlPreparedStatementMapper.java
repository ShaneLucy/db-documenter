package db.documenter.internal.queries.impl.postgresql.preparedstatements;

import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.api.PreparedStatementMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
      final PreparedStatement preparedStatement, final String schema, final Table table)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, table.name());
  }

  @Override
  public void prepareForeignKeyInfoStatement(
      final PreparedStatement preparedStatement, final String schema, final Table table)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, table.name());
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
}
