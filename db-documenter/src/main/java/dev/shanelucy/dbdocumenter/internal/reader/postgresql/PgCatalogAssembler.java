package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import dev.shanelucy.dbdocumenter.catalog.EnumType;
import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.Schema;
import dev.shanelucy.dbdocumenter.catalog.Table;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ColumnRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ConstraintRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.EnumRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TableRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.UniqueIndexRow;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Joins the rows of one {@link PgCatalogReader} run into a {@link CatalogSnapshot}.
 *
 * <p>Rows are joined by table OID and schema name; each table is built by {@link PgTableAssembler}.
 * The reader's queries are separate statements, so concurrent DDL between them can leave rows that
 * no longer fit together. Such rows are dropped and logged at {@code WARNING} rather than thrown
 * (plan decision S10):
 *
 * <ul>
 *   <li>a table whose schema is not among the schema rows
 *   <li>a table whose column rows do not match its column count, which means it was dropped or
 *       altered between the tables and columns queries (a genuine zero-column table is kept)
 *   <li>a column, constraint or unique index whose table is not among the table rows
 *   <li>a key or index naming a column that is not among its table's column rows
 *   <li>an enum whose schema is not among the schema rows
 * </ul>
 *
 * <p>Each requested schema absent from the snapshot is also logged at {@code WARNING}, saying
 * whether it does not exist or lacks {@code USAGE}; it stays absent.
 *
 * <p>Kept free of JDBC so the joining rules can be unit-tested without a database.
 */
final class PgCatalogAssembler {

  private PgCatalogAssembler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Builds the snapshot from the rows of one run.
   *
   * @param productName the product name reported by the driver
   * @param productVersion the product version reported by the driver
   * @param rows the rows read
   * @return the snapshot with one schema per schema row, each holding its tables and enums
   * @throws IllegalArgumentException if a foreign key has no referenced table or an unknown
   *     referential action code, or the rows break a model invariant that concurrent DDL cannot
   *     explain
   */
  static CatalogSnapshot assemble(
      final String productName, final String productVersion, final PgCatalogRows rows) {
    final Set<String> schemaNames = Set.copyOf(rows.schemaNames());
    logAbsentSchemas(rows, schemaNames);
    final Map<Long, TableRow> tablesByOid =
        rows.tables().stream()
            .filter(table -> isInSchemas(schemaNames, table.schema(), "table", table.name()))
            .collect(Collectors.toMap(TableRow::oid, Function.identity()));
    final Map<Long, List<ColumnRow>> columnsByTable =
        groupByTable(tablesByOid, rows.columns(), ColumnRow::tableOid, ColumnRow::name, "column");
    final Map<Long, List<ConstraintRow>> constraintsByTable =
        groupByTable(
            tablesByOid,
            rows.constraints(),
            ConstraintRow::tableOid,
            ConstraintRow::name,
            "constraint");
    final Map<Long, List<UniqueIndexRow>> indexesByTable =
        groupByTable(
            tablesByOid,
            rows.uniqueIndexes(),
            UniqueIndexRow::tableOid,
            UniqueIndexRow::name,
            "unique index");

    final Map<String, List<Table>> tablesBySchema =
        tablesByOid.values().stream()
            .filter(table -> hasAllColumns(table, columnsByTable))
            .map(
                table ->
                    PgTableAssembler.toTable(
                        table,
                        columnsByTable.getOrDefault(table.oid(), List.of()),
                        constraintsByTable.getOrDefault(table.oid(), List.of()),
                        indexesByTable.getOrDefault(table.oid(), List.of())))
            .collect(Collectors.groupingBy(table -> table.name().schema()));
    final Map<String, List<EnumType>> enumsBySchema =
        rows.enums().stream()
            .filter(enumRow -> isInSchemas(schemaNames, enumRow.schema(), "enum", enumRow.name()))
            .map(PgCatalogAssembler::toEnumType)
            .collect(Collectors.groupingBy(enumType -> enumType.name().schema()));

    final List<Schema> schemas =
        schemaNames.stream()
            .map(
                name ->
                    Schema.builder()
                        .name(name)
                        .tables(tablesBySchema.getOrDefault(name, List.of()))
                        .enumTypes(enumsBySchema.getOrDefault(name, List.of()))
                        .build())
            .toList();
    return CatalogSnapshot.builder()
        .productName(productName)
        .productVersion(productVersion)
        .schemas(schemas)
        .build();
  }

  private static void logAbsentSchemas(final PgCatalogRows rows, final Set<String> schemaNames) {
    final Set<String> withoutUsage = Set.copyOf(rows.schemasWithoutUsage());
    rows.requestedSchemas().stream()
        .filter(schema -> !schemaNames.contains(schema))
        .forEach(
            schema ->
                PgCatalogLog.schemaAbsent(
                    schema,
                    withoutUsage.contains(schema)
                        ? "the role has no USAGE privilege on it"
                        : "it does not exist"));
  }

  /**
   * Groups rows by table OID, dropping rows whose table is unknown. Encounter order is kept within
   * each group, so columns stay in {@code attnum} order.
   */
  private static <T> Map<Long, List<T>> groupByTable(
      final Map<Long, TableRow> tablesByOid,
      final List<T> rows,
      final Function<T, Long> tableOid,
      final Function<T, String> name,
      final String kind) {
    return rows.stream()
        .filter(row -> isKnownTable(tablesByOid, tableOid.apply(row), kind, name.apply(row)))
        .collect(Collectors.groupingBy(tableOid));
  }

  private static boolean hasAllColumns(
      final TableRow table, final Map<Long, List<ColumnRow>> columnsByTable) {
    final int columnRows = columnsByTable.getOrDefault(table.oid(), List.of()).size();
    if (columnRows == table.columnCount()) {
      return true;
    }
    PgCatalogLog.dropped(
        "table",
        table.schema() + "." + table.name(),
        "it had " + table.columnCount() + " columns but " + columnRows + " column rows were read");
    return false;
  }

  private static EnumType toEnumType(final EnumRow row) {
    return EnumType.builder()
        .name(QualifiedName.of(row.schema(), row.name()))
        .values(row.labels())
        .build();
  }

  private static boolean isInSchemas(
      final Set<String> schemaNames, final String schema, final String kind, final String name) {
    if (schemaNames.contains(schema)) {
      return true;
    }
    PgCatalogLog.dropped(kind, schema + "." + name, "its schema was not read");
    return false;
  }

  private static boolean isKnownTable(
      final Map<Long, TableRow> tablesByOid, final long oid, final String kind, final String name) {
    if (tablesByOid.containsKey(oid)) {
      return true;
    }
    PgCatalogLog.dropped(kind, name, "its table (OID " + oid + ") was not read");
    return false;
  }
}
