package db.documenter.internal.mapper;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.util.List;
import java.util.Optional;

/** Maps raw table metadata components into table instances. */
public final class TableMapper {

  /**
   * Combines table components into a table instance.
   *
   * @param tableName the name of the table
   * @param columns the list of columns
   * @param primaryKey the primary key, or empty if the table has none
   * @param foreignKeys the list of foreign keys
   * @return {@link Table} instance
   */
  public Table combineTableComponents(
      final String tableName,
      final List<Column> columns,
      final Optional<PrimaryKey> primaryKey,
      final List<ForeignKey> foreignKeys) {
    return Table.builder()
        .name(tableName)
        .columns(columns)
        .primaryKey(primaryKey)
        .foreignKeys(foreignKeys)
        .build();
  }
}
