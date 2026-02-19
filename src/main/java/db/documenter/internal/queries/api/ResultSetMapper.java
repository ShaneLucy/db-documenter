package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.*;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ResultSetMapper {

  List<Table> mapToTables(ResultSet resultSet) throws SQLException;

  List<Column> mapToColumns(ResultSet resultSet) throws SQLException;

  PrimaryKey mapToPrimaryKey(ResultSet resultSet) throws SQLException;

  List<ForeignKey> mapToForeignKeys(ResultSet resultSet) throws SQLException;

  List<DbEnum> mapToDbEnumInfo(ResultSet resultSet) throws SQLException;

  List<String> mapToDbEnumValues(ResultSet resultSet) throws SQLException;

  Map<ColumnKey, UdtReference> mapToColumnUdtMappings(ResultSet resultSet) throws SQLException;

  List<DbCompositeType> mapToDbCompositeTypeInfo(ResultSet resultSet) throws SQLException;
}
