package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Table;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementMapper {

  void prepareTableInfoStatement(PreparedStatement preparedStatement, String schema)
      throws SQLException;

  void prepareColumnInfoStatement(
      PreparedStatement preparedStatement, String schema, String tableName) throws SQLException;

  void preparePrimaryKeyInfoStatement(
      PreparedStatement preparedStatement, String schema, Table table) throws SQLException;

  void prepareForeignKeyInfoStatement(
      PreparedStatement preparedStatement, String schema, Table table) throws SQLException;
}
