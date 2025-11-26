package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface ResultSetMapper {

  List<Table> mapToTables(ResultSet resultSet) throws SQLException;

  List<Column> mapToColumns(ResultSet resultSet) throws SQLException;

  PrimaryKey mapToPrimaryKey(ResultSet resultSet) throws SQLException;

  List<ForeignKey> mapToForeignKeys(ResultSet resultSet) throws SQLException;
}
