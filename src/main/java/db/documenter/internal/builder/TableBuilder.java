package db.documenter.internal.builder;

import db.documenter.internal.mapper.ColumnMapper;
import db.documenter.internal.mapper.ColumnMappingContext;
import db.documenter.internal.mapper.ForeignKeyMapper;
import db.documenter.internal.mapper.TableMapper;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.api.QueryRunner;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Builds database table information from schema metadata. */
public final class TableBuilder {
  private static final Logger LOGGER = Logger.getLogger(TableBuilder.class.getName());
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
   * @param enumsByKey map of enum keys to enum definitions for O(1) lookup
   * @param columnUdtMappings map of column keys to UDT references
   * @return list of {@link Table} instances
   * @throws SQLException if database access fails
   */
  public List<Table> buildTables(
      final QueryRunner queryRunner,
      final String schema,
      final Map<EnumKey, DbEnum> enumsByKey,
      final Map<ColumnKey, UdtReference> columnUdtMappings)
      throws SQLException {
    final List<Table> tables = queryRunner.getTableInfo(schema);

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Building tables for schema: {0}", LogUtils.sanitizeForLog(schema));
    }

    final List<Table> result = new ArrayList<>();

    for (final Table table : tables) {
      final List<Column> rawColumns = queryRunner.getColumnInfo(schema, table);

      // Context objects are intentionally created per-table for proper UDT resolution
      @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
      final ColumnMappingContext context =
          new ColumnMappingContext(columnUdtMappings, enumsByKey, table.name(), schema);

      final List<Column> columnsWithEnumTypes =
          columnMapper.mapUserDefinedTypes(rawColumns, context);

      final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo(schema, table);

      final List<ForeignKey> rawForeignKeys = queryRunner.getForeignKeyInfo(schema, table);
      final List<ForeignKey> foreignKeys =
          foreignKeyMapper.enrichWithNullability(rawForeignKeys, columnsWithEnumTypes);

      final List<Column> columns =
          columnMapper.enrichWithForeignKeyConstraints(columnsWithEnumTypes, rawForeignKeys);

      result.add(
          tableMapper.combineTableComponents(table.name(), columns, primaryKey, foreignKeys));
    }

    return result;
  }
}
