package db.documenter.internal.mapper;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Maps raw table metadata components into table instances. */
public final class TableMapper {

  /**
   * Combines table components into a table instance.
   *
   * @param tableName the name of the table
   * @param columns the list of columns
   * @param primaryKey the primary key
   * @param foreignKeys the list of foreign keys
   * @param partitionStrategy the PostgreSQL partition key expression (e.g., {@code "RANGE
   *     (stat_date)"}), or {@code null} for non-partitioned tables
   * @param partitionNames ordered list of child partition names; empty for non-partitioned tables
   * @return {@link Table} instance
   */
  public Table combineTableComponents(
      final String tableName,
      final List<Column> columns,
      final PrimaryKey primaryKey,
      final List<ForeignKey> foreignKeys,
      final @Nullable String partitionStrategy,
      final List<String> partitionNames) {
    return Table.builder()
        .name(tableName)
        .columns(columns)
        .primaryKey(primaryKey)
        .foreignKeys(foreignKeys)
        .partitionStrategy(partitionStrategy)
        .partitionNames(partitionNames)
        .build();
  }
}
