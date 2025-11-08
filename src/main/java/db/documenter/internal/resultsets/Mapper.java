package db.documenter.internal.resultsets;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class Mapper {


    public static Table mapToTable(final ResultSet resultSet) throws SQLException {
        return Table.builder()
                .schema(resultSet.getString("table_schema"))
                .name(resultSet.getString("table_name"))
                .type(resultSet.getString("table_type")).build();
    }

    public static Column mapToColumn(final ResultSet resultSet) throws SQLException {
        return Column.builder()
                .name(resultSet.getString("column_name"))
                .ordinalPosition(resultSet.getInt("ordinal_position"))
                .isNullable(Objects.equals(resultSet.getString("is_nullable"), "YES"))
                .dataType(resultSet.getString("data_type"))
                .maximumLength(resultSet.getInt("character_maximum_length")).build();
    }


    public static Table combineTableAndColumns(final Table table, final List<Column> columns) {
        return Table.builder()
                .schema(table.schema())
                .name(table.name())
                .type(table.type())
                .columns(columns)
                .build();
    }
}


