package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface ResultSetMapper {

  List<Table> mapToTables(ResultSet resultSet) throws SQLException;

  List<Column> mapToColumns(ResultSet resultSet) throws SQLException;

  PrimaryKey mapToPrimaryKey(ResultSet resultSet) throws SQLException;

  List<ForeignKey> mapToForeignKeys(ResultSet resultSet) throws SQLException;

  List<DbEnum> mapToDbEnumInfo(ResultSet resultSet) throws SQLException;

  List<String> mapToDbEnumValues(ResultSet resultSet) throws SQLException;
}
