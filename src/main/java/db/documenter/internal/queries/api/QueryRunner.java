package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.sql.SQLException;
import java.util.List;

public interface QueryRunner {

  List<Table> getTableInfo(String schema) throws SQLException;

  List<Column> getColumnInfo(String schema, Table table) throws SQLException;

  PrimaryKey getPrimaryKeyInfo(String schema, Table table) throws SQLException;

  List<ForeignKey> getForeignKeyInfo(String schema, Table table) throws SQLException;

  List<DbEnum> getEnumInfo(String schema) throws SQLException;

  List<String> getEnumValues(String schema, DbEnum dbEnums) throws SQLException;
}
