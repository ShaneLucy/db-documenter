package db.documenter.internal.queries.impl.postgresql.resultsets;

import db.documenter.internal.models.db.*;
import db.documenter.internal.queries.api.ResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PostgresqlResultSetMapper implements ResultSetMapper {

  @Override
  public List<Table> mapToTables(final ResultSet resultSet) throws SQLException {
    final List<Table> tables = new ArrayList<>();
    while (resultSet.next()) {
      tables.add(
          Table.builder()
              .name(resultSet.getString("table_name"))
              .columns(List.of())
              .foreignKeys(List.of())
              .build());
    }
    return tables;
  }

  @Override
  public List<Column> mapToColumns(final ResultSet resultSet) throws SQLException {
    final List<Column> columns = new ArrayList<>();
    while (resultSet.next()) {
      final List<Constraint> constraints = buildConstraints(resultSet);

      columns.add(
          Column.builder()
              .name(resultSet.getString("column_name"))
              .dataType(resultSet.getString("data_type"))
              .maximumLength(resultSet.getInt("character_maximum_length"))
              .constraints(constraints)
              .build());
    }
    return columns;
  }

  private List<Constraint> buildConstraints(final ResultSet resultSet) throws SQLException {
    final List<Constraint> constraints = new ArrayList<>();

    if (resultSet.getBoolean("is_unique")) {
      constraints.add(Constraint.UNIQUE);
    }

    final var checkConstraint = resultSet.getString("check_constraint");
    if (checkConstraint != null && !checkConstraint.isBlank()) {
      constraints.add(Constraint.CHECK);
    }

    final var defaultValue = resultSet.getString("column_default");
    if (defaultValue != null && !defaultValue.isBlank()) {
      constraints.add(Constraint.DEFAULT);
    }

    if (resultSet.getBoolean("is_auto_increment")) {
      constraints.add(Constraint.AUTO_INCREMENT);
    }

    if (Objects.equals(resultSet.getString("is_nullable"), "YES")) {
      constraints.add(Constraint.NULLABLE);
    }

    return constraints;
  }

  @Override
  public PrimaryKey mapToPrimaryKey(final ResultSet resultSet) throws SQLException {
    if (!resultSet.next()) {
      return null;
    }

    final var primaryKeyBuilder =
        PrimaryKey.builder().constraintName(resultSet.getString("constraint_name"));

    final List<String> columnNames = new ArrayList<>();
    columnNames.add(resultSet.getString("column_name"));

    while (resultSet.next()) {
      columnNames.add(resultSet.getString("column_name"));
    }

    return primaryKeyBuilder.columnNames(columnNames).build();
  }

  @Override
  public List<ForeignKey> mapToForeignKeys(final ResultSet resultSet) throws SQLException {
    final List<ForeignKey> foreignKeys = new ArrayList<>();

    while (resultSet.next()) {
      foreignKeys.add(
          ForeignKey.builder()
              .name(resultSet.getString("constraint_name"))
              .sourceTable(resultSet.getString("source_table_name"))
              .sourceColumn(resultSet.getString("source_column"))
              .targetTable(resultSet.getString("referenced_table"))
              .targetColumn(resultSet.getString("referenced_column"))
              .referencedSchema(resultSet.getString("referenced_schema"))
              .build());
    }

    return foreignKeys;
  }

  @Override
  public List<DbEnum> mapToDbEnumInfo(final ResultSet resultSet) throws SQLException {
    final List<DbEnum> dbEnums = new ArrayList<>();

    while (resultSet.next()) {
      dbEnums.add(
          DbEnum.builder()
              .columnName(resultSet.getString("column_name"))
              .enumName(resultSet.getString("udt_name"))
              .enumValues(List.of())
              .build());
    }

    return dbEnums;
  }

  @Override
  public List<String> mapToDbEnumValues(final ResultSet resultSet) throws SQLException {
    final List<String> dbEnumValues = new ArrayList<>();

    while (resultSet.next()) {
      dbEnumValues.add(resultSet.getString("enumlabel"));
    }

    return dbEnumValues;
  }
}
