package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ColumnRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ConstraintRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.EnumRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TableRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TypeRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.UniqueIndexRow;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Maps the current row of each {@link PgCatalogQueries} result set to its {@link PgCatalogRows} row
 * record, by column label.
 *
 * <p>The mappers only read the current row; they never advance or close the result set.
 */
final class PgCatalogRowMappers {

  private PgCatalogRowMappers() {
    throw new IllegalStateException("Utility class");
  }

  /** Maps a {@code PgCatalogQueries.TABLES} row. */
  static TableRow mapTable(final ResultSet resultSet) throws SQLException {
    return new TableRow(
        resultSet.getLong("table_oid"),
        resultSet.getString("schema_name"),
        resultSet.getString("table_name"),
        resultSet.getInt("column_count"));
  }

  /** Maps a {@code PgCatalogQueries.COLUMNS} row. */
  static ColumnRow mapColumn(final ResultSet resultSet) throws SQLException {
    final TypeRow type =
        new TypeRow(
            resultSet.getString("formatted_type"),
            resultSet.getString("type_schema"),
            resultSet.getString("type_name"),
            nullableString(resultSet, "element_schema"),
            nullableString(resultSet, "element_name"));
    return new ColumnRow(
        resultSet.getLong("table_oid"),
        resultSet.getString("column_name"),
        resultSet.getBoolean("is_nullable"),
        type);
  }

  /**
   * Maps a {@code PgCatalogQueries.CONSTRAINTS} row.
   *
   * @throws IllegalArgumentException if the {@code contype} code is not {@code p}, {@code u} or
   *     {@code f}
   */
  static ConstraintRow mapConstraint(final ResultSet resultSet) throws SQLException {
    final @Nullable String referencedSchema = nullableString(resultSet, "referenced_schema");
    final @Nullable String referencedTable = nullableString(resultSet, "referenced_table");
    return ConstraintRow.builder()
        .tableOid(resultSet.getLong("table_oid"))
        .name(resultSet.getString("constraint_name"))
        .type(ConstraintType.fromCode(code(resultSet, "constraint_type")))
        .columns(textArray(resultSet, "column_names"))
        .referencedTable(
            referencedSchema == null || referencedTable == null
                ? null
                : QualifiedName.of(referencedSchema, referencedTable))
        .referencedColumns(textArray(resultSet, "referenced_column_names"))
        .onDelete(code(resultSet, "on_delete"))
        .onUpdate(code(resultSet, "on_update"))
        .build();
  }

  /** Maps a {@code PgCatalogQueries.UNIQUE_INDEXES} row. */
  static UniqueIndexRow mapUniqueIndex(final ResultSet resultSet) throws SQLException {
    return new UniqueIndexRow(
        resultSet.getLong("table_oid"),
        resultSet.getString("index_name"),
        textArray(resultSet, "column_names"));
  }

  /** Maps a {@code PgCatalogQueries.ENUMS} row. */
  static EnumRow mapEnum(final ResultSet resultSet) throws SQLException {
    return new EnumRow(
        resultSet.getString("schema_name"),
        resultSet.getString("enum_name"),
        textArray(resultSet, "labels"));
  }

  private static @Nullable String nullableString(final ResultSet resultSet, final String column)
      throws SQLException {
    return resultSet.getString(column);
  }

  /** Reads a single-character {@code "char"} code; an empty or null value reads as a space. */
  private static char code(final ResultSet resultSet, final String column) throws SQLException {
    final @Nullable String value = resultSet.getString(column);
    return value == null || value.isEmpty() ? ' ' : value.charAt(0);
  }

  /** Reads a {@code text[]} column, freeing the driver's array; SQL NULL reads as empty. */
  private static List<String> textArray(final ResultSet resultSet, final String column)
      throws SQLException {
    final @Nullable Array array = resultSet.getArray(column);
    if (array == null) {
      return List.of();
    }
    try {
      if (array.getArray() instanceof final String[] values) {
        return Arrays.asList(values);
      }
      throw new SQLException("Expected a text[] value in column " + column);
    } finally {
      array.free();
    }
  }

  /**
   * Maps the current row of a result set.
   *
   * @param <T> the row record type
   */
  @FunctionalInterface
  interface RowMapper<T> {

    /**
     * Maps the current row.
     *
     * @param resultSet a result set positioned on a row
     * @return the mapped row; never null
     * @throws SQLException if a column cannot be read
     */
    T map(ResultSet resultSet) throws SQLException;
  }
}
