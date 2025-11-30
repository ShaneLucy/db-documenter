package db.documenter.internal.builder;

import db.documenter.internal.mapper.ColumnMapper;
import db.documenter.internal.mapper.ForeignKeyMapper;
import db.documenter.internal.mapper.TableMapper;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.api.QueryRunner;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Builds database table information from schema metadata. */
public final class TableBuilder {
  private final ColumnMapper columnMapper;
  private final ForeignKeyMapper foreignKeyMapper;
  private final TableMapper tableMapper;

  public TableBuilder(
      final ColumnMapper columnMapper,
      final ForeignKeyMapper foreignKeyMapper,
      final TableMapper tableMapper) {
    this.columnMapper = columnMapper;
    this.foreignKeyMapper = foreignKeyMapper;
    this.tableMapper = tableMapper;
  }

  /**
   * Builds a list of tables with columns, primary keys, and foreign keys for a given schema.
   *
   * @param queryRunner the query runner to fetch table metadata
   * @param schema the schema name
   * @param dbEnums the list of database enums for mapping USER-DEFINED types
   * @return list of {@link Table} instances
   * @throws SQLException if database access fails
   */
  public List<Table> buildTables(
      final QueryRunner queryRunner, final String schema, final List<DbEnum> dbEnums)
      throws SQLException {
    final List<Table> tables = queryRunner.getTableInfo(schema);
    final List<Table> result = new ArrayList<>();

    for (final Table table : tables) {
      final List<Column> rawColumns = queryRunner.getColumnInfo(schema, table);
      final List<Column> columns = columnMapper.mapUserDefinedTypes(rawColumns, dbEnums);

      final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo(schema, table);

      final List<ForeignKey> rawForeignKeys = queryRunner.getForeignKeyInfo(schema, table);
      final List<ForeignKey> foreignKeys =
          foreignKeyMapper.enrichWithNullability(rawForeignKeys, columns);

      result.add(
          tableMapper.combineTableComponents(table.name(), columns, primaryKey, foreignKeys));
    }

    return result;
  }
}
