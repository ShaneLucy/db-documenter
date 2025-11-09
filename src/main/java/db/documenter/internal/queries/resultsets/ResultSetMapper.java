package db.documenter.internal.queries.resultsets;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResultSetMapper {

  public Table mapToTable(final ResultSet resultSet) throws SQLException {
    return Table.builder()
        .schema(resultSet.getString("table_schema"))
        .name(resultSet.getString("table_name"))
        .type(resultSet.getString("table_type"))
        .build();
  }

  public Column mapToColumn(final ResultSet resultSet) throws SQLException {
    return Column.builder()
        .name(resultSet.getString("column_name"))
        .ordinalPosition(resultSet.getInt("ordinal_position"))
        .isNullable(Objects.equals(resultSet.getString("is_nullable"), "YES"))
        .dataType(resultSet.getString("data_type"))
        .maximumLength(resultSet.getInt("character_maximum_length"))
        .build();
  }

  public PrimaryKey mapToPrimaryKey(final ResultSet resultSet) throws SQLException {
    if (!resultSet.next()) {
      return null;
    }

    final ArrayList<String> columnNames = new ArrayList<>();
    columnNames.add(resultSet.getString("column_name"));
    final var primaryKey =
        PrimaryKey.builder()
            .columnNames(columnNames)
            .constraintName(resultSet.getString("constraint_name"))
            .build();

    while (resultSet.next()) {
      primaryKey.columnNames().add(resultSet.getString("column_name"));
    }

    return primaryKey;
  }

  public ForeignKey mapToForeignKey(final ResultSet resultSet) throws SQLException {
    return ForeignKey.builder()
        .name(resultSet.getString("constraint_name"))
        .sourceTable(resultSet.getString("source_table_name"))
        .sourceColumn(resultSet.getString("source_column"))
        .targetTable(resultSet.getString("referenced_table"))
        .targetColumn(resultSet.getString("referenced_column"))
        .build();
  }

  public Table combineTableColumnsPrimaryAndForeignKeys(
      final Table table,
      final List<Column> columns,
      final PrimaryKey primaryKey,
      final List<ForeignKey> foreignKeys) {
    return Table.builder()
        .schema(table.schema())
        .name(table.name())
        .type(table.type())
        .columns(columns)
        .primaryKey(primaryKey)
        .foreignKeys(foreignKeys)
        .build();
  }
}
