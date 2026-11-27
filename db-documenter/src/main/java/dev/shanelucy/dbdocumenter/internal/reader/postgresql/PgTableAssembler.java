package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.catalog.Column;
import dev.shanelucy.dbdocumenter.catalog.ForeignKey;
import dev.shanelucy.dbdocumenter.catalog.PrimaryKey;
import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.Table;
import dev.shanelucy.dbdocumenter.catalog.UniqueKey;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ColumnRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ConstraintRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TableRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.UniqueIndexRow;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Builds one {@link Table} from its table, column, constraint and unique-index rows.
 *
 * <p>A key or unique index is left out, with a warning, when:
 *
 * <ul>
 *   <li>it names a column that is not among the table's column rows (concurrent DDL)
 *   <li>it lists a column twice, which PostgreSQL allows for a unique index ({@code (a, a)}) but a
 *       {@link UniqueKey} cannot represent; such an index is unique on its distinct columns anyway
 * </ul>
 *
 * <p>Kept free of JDBC so the rules can be unit-tested without a database.
 */
final class PgTableAssembler {

  private PgTableAssembler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Builds the table.
   *
   * @param tableRow the table
   * @param columnRows its columns, in {@code attnum} order
   * @param constraintRows its constraints
   * @param indexRows its unique indexes
   * @return the table
   * @throws IllegalArgumentException if a foreign key has no referenced table or an unknown
   *     referential action code
   */
  static Table toTable(
      final TableRow tableRow,
      final List<ColumnRow> columnRows,
      final List<ConstraintRow> constraintRows,
      final List<UniqueIndexRow> indexRows) {
    final QualifiedName tableName = QualifiedName.of(tableRow.schema(), tableRow.name());
    final List<Column> columns = columnRows.stream().map(PgTableAssembler::toColumn).toList();
    final Set<String> columnNames = columns.stream().map(Column::name).collect(Collectors.toSet());
    final List<ConstraintRow> constraints =
        constraintRows.stream()
            .filter(row -> isUsableKey(columnNames, row.columns(), tableName, row.name()))
            .toList();
    final Stream<UniqueKey> indexKeys =
        indexRows.stream()
            .filter(row -> isUsableKey(columnNames, row.columns(), tableName, row.name()))
            .map(row -> uniqueKey(row.name(), row.columns()));
    final Stream<UniqueKey> constraintKeys =
        ofType(constraints, ConstraintType.UNIQUE).map(row -> uniqueKey(row.name(), row.columns()));
    return Table.builder()
        .name(tableName)
        .columns(columns)
        .primaryKey(primaryKey(constraints))
        .uniqueKeys(Stream.concat(constraintKeys, indexKeys).toList())
        .foreignKeys(
            ofType(constraints, ConstraintType.FOREIGN_KEY)
                .map(PgTableAssembler::toForeignKey)
                .toList())
        .build();
  }

  private static Stream<ConstraintRow> ofType(
      final List<ConstraintRow> constraints, final ConstraintType type) {
    return constraints.stream().filter(row -> row.type() == type);
  }

  private static @Nullable PrimaryKey primaryKey(final List<ConstraintRow> constraints) {
    return ofType(constraints, ConstraintType.PRIMARY_KEY)
        .findFirst()
        .map(row -> PrimaryKey.builder().name(row.name()).columns(row.columns()).build())
        .orElse(null);
  }

  private static UniqueKey uniqueKey(final String name, final List<String> columns) {
    return UniqueKey.builder().name(name).columns(columns).build();
  }

  private static ForeignKey toForeignKey(final ConstraintRow row) {
    final QualifiedName referencedTable = row.referencedTable();
    if (referencedTable == null) {
      throw new IllegalArgumentException("Foreign key " + row.name() + " has no referenced table");
    }
    return ForeignKey.builder()
        .name(row.name())
        .columns(row.columns())
        .referencedTable(referencedTable)
        .referencedColumns(row.referencedColumns())
        .onDelete(PgCatalogMappings.referentialAction(row.onDelete()))
        .onUpdate(PgCatalogMappings.referentialAction(row.onUpdate()))
        .build();
  }

  private static Column toColumn(final ColumnRow row) {
    return Column.builder()
        .name(row.name())
        .nullable(row.nullable())
        .dataType(PgCatalogMappings.dataType(row.type()))
        .build();
  }

  /**
   * Returns whether a key's columns are all known and distinct, logging why not otherwise.
   * PostgreSQL rejects a column listed twice in a constraint, so in practice only unique indexes
   * fail the second check.
   */
  private static boolean isUsableKey(
      final Set<String> columnNames,
      final List<String> keyColumns,
      final QualifiedName table,
      final String name) {
    if (!columnNames.containsAll(keyColumns)) {
      PgCatalogLog.dropped("key", table + " " + name, "it names a column that was not read");
      return false;
    }
    if (new HashSet<>(keyColumns).size() != keyColumns.size()) {
      PgCatalogLog.skipped("key", table + " " + name, "it lists a column more than once");
      return false;
    }
    return true;
  }
}
