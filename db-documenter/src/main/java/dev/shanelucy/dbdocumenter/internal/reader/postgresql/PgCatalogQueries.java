package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRowMappers.RowMapper;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The six {@code pg_catalog} statements {@link PgCatalogReader} runs, each with a single {@code ?}
 * bound to the requested schema names as a {@code text[]}.
 *
 * <p><b>Session independence:</b> every catalog table, function, cast and operator is qualified
 * with {@code pg_catalog} (operators as {@code OPERATOR(pg_catalog.=)}, including {@code IN} lists
 * written as {@code OPERATOR(pg_catalog.=) ANY (…)}), so objects in {@code search_path}, including
 * the session's temporary schema, cannot change how a query resolves. User type names are read from
 * the raw catalog columns, never from {@code format_type} or a {@code reg*} cast. The statements
 * are private and run by {@link #readRows}, so their text is not copied into other class files.
 */
final class PgCatalogQueries {

  /**
   * The tables the role may document, shared by every per-table query so they all apply the same
   * scope and visibility rules. {@code has_any_column_privilege} is also true for a table-level
   * grant.
   */
  private static final String VISIBLE_TABLES_CTE =
      """
      WITH visible_tables AS (
        SELECT c.oid, n.nspname, c.relname
        FROM pg_catalog.pg_class c
        JOIN pg_catalog.pg_namespace n ON n.oid OPERATOR(pg_catalog.=) c.relnamespace
        WHERE n.nspname OPERATOR(pg_catalog.=) ANY (?)
          AND c.relkind OPERATOR(pg_catalog.=) ANY ('{r,p}'::pg_catalog."char"[])
          AND NOT c.relispartition
          AND pg_catalog.has_schema_privilege(n.oid, 'USAGE')
          AND pg_catalog.has_any_column_privilege(c.oid, 'SELECT')
      )
      """;

  /** Requested schemas that exist, and whether the role has {@code USAGE} on each. */
  private static final String SCHEMAS =
      """
      SELECT n.nspname AS schema_name,
             pg_catalog.has_schema_privilege(n.oid, 'USAGE') AS has_usage
      FROM pg_catalog.pg_namespace n
      WHERE n.nspname OPERATOR(pg_catalog.=) ANY (?)
      """;

  /**
   * Visible tables, with their live column count so the assembler can detect a table dropped or
   * altered before the columns query ran.
   */
  private static final String TABLES =
      VISIBLE_TABLES_CTE
          + """
          SELECT vt.oid AS table_oid,
                 vt.nspname AS schema_name,
                 vt.relname AS table_name,
                 (
                   SELECT pg_catalog.count(*)
                   FROM pg_catalog.pg_attribute a
                   WHERE a.attrelid OPERATOR(pg_catalog.=) vt.oid
                     AND a.attnum OPERATOR(pg_catalog.>) 0
                     AND NOT a.attisdropped
                 ) AS column_count
          FROM visible_tables vt
          """;

  /**
   * Columns in {@code attnum} order.
   *
   * <p>The element join recognises a true array type as one whose element type names it as its
   * array type ({@code typarray}), which excludes types such as {@code point} that have a {@code
   * typelem} but are not arrays.
   *
   * <p>A column is not nullable when it has {@code attnotnull} or its type is a domain whose chain
   * (domain over domain, followed through {@code typbasetype}) has any {@code typnotnull}. An array
   * of such a domain stays nullable, because the array itself may be NULL.
   */
  private static final String COLUMNS =
      VISIBLE_TABLES_CTE
          + """
          SELECT a.attrelid AS table_oid,
                 a.attname AS column_name,
                 NOT (
                   a.attnotnull
                   OR COALESCE((
                     WITH RECURSIVE domain_chain AS (
                       SELECT t.typnotnull, t.typbasetype
                       WHERE t.typtype OPERATOR(pg_catalog.=) 'd'
                       UNION ALL
                       SELECT d.typnotnull, d.typbasetype
                       FROM pg_catalog.pg_type d
                       JOIN domain_chain dc ON d.oid OPERATOR(pg_catalog.=) dc.typbasetype
                       WHERE d.typtype OPERATOR(pg_catalog.=) 'd'
                     )
                     SELECT pg_catalog.bool_or(dc.typnotnull) FROM domain_chain dc
                   ), false)
                 ) AS is_nullable,
                 pg_catalog.format_type(a.atttypid, a.atttypmod) AS formatted_type,
                 tn.nspname AS type_schema,
                 t.typname AS type_name,
                 en.nspname AS element_schema,
                 et.typname AS element_name
          FROM visible_tables vt
          JOIN pg_catalog.pg_attribute a ON a.attrelid OPERATOR(pg_catalog.=) vt.oid
          JOIN pg_catalog.pg_type t ON t.oid OPERATOR(pg_catalog.=) a.atttypid
          JOIN pg_catalog.pg_namespace tn ON tn.oid OPERATOR(pg_catalog.=) t.typnamespace
          LEFT JOIN pg_catalog.pg_type et
            ON et.oid OPERATOR(pg_catalog.=) t.typelem
           AND et.typarray OPERATOR(pg_catalog.=) t.oid
          LEFT JOIN pg_catalog.pg_namespace en ON en.oid OPERATOR(pg_catalog.=) et.typnamespace
          WHERE a.attnum OPERATOR(pg_catalog.>) 0
            AND NOT a.attisdropped
          ORDER BY a.attrelid, a.attnum
          """;

  /**
   * Primary-key, unique and foreign-key constraints, with key columns resolved by attnum in key
   * order. Constraints cloned onto partitions ({@code conparentid <> 0}) are skipped. The
   * referenced table and columns are resolved through {@code pg_class} and {@code pg_attribute}
   * without a visibility filter, so out-of-scope and invisible targets keep their names.
   */
  private static final String CONSTRAINTS =
      VISIBLE_TABLES_CTE
          + """
          SELECT con.conrelid AS table_oid,
                 con.conname AS constraint_name,
                 con.contype AS constraint_type,
                 ARRAY(
                   SELECT a.attname::pg_catalog.text
                   FROM pg_catalog.unnest(con.conkey) WITH ORDINALITY AS k(attnum, key_position)
                   JOIN pg_catalog.pg_attribute a
                     ON a.attrelid OPERATOR(pg_catalog.=) con.conrelid
                    AND a.attnum OPERATOR(pg_catalog.=) k.attnum
                   ORDER BY k.key_position
                 ) AS column_names,
                 rn.nspname AS referenced_schema,
                 rc.relname AS referenced_table,
                 ARRAY(
                   SELECT a.attname::pg_catalog.text
                   FROM pg_catalog.unnest(con.confkey) WITH ORDINALITY AS k(attnum, key_position)
                   JOIN pg_catalog.pg_attribute a
                     ON a.attrelid OPERATOR(pg_catalog.=) con.confrelid
                    AND a.attnum OPERATOR(pg_catalog.=) k.attnum
                   ORDER BY k.key_position
                 ) AS referenced_column_names,
                 con.confdeltype AS on_delete,
                 con.confupdtype AS on_update
          FROM visible_tables vt
          JOIN pg_catalog.pg_constraint con ON con.conrelid OPERATOR(pg_catalog.=) vt.oid
          LEFT JOIN pg_catalog.pg_class rc ON rc.oid OPERATOR(pg_catalog.=) con.confrelid
          LEFT JOIN pg_catalog.pg_namespace rn ON rn.oid OPERATOR(pg_catalog.=) rc.relnamespace
          WHERE con.contype OPERATOR(pg_catalog.=) ANY ('{p,u,f}'::pg_catalog."char"[])
            AND con.conparentid OPERATOR(pg_catalog.=) 0
          """;

  /**
   * Unique indexes that are unique keys but not constraints (plan decision B2): valid, not partial,
   * no expression keys ({@code 0} in {@code indkey}), not backing a primary-key or unique
   * constraint, and only the first {@code indnkeyatts} (non-{@code INCLUDE}) columns. Indexes on
   * partitioned tables ({@code relkind 'I'}) are read like any other; an {@code ON ONLY} index is
   * invalid until every partition has one, so it is skipped.
   */
  private static final String UNIQUE_INDEXES =
      VISIBLE_TABLES_CTE
          + """
          SELECT i.indrelid AS table_oid,
                 ic.relname AS index_name,
                 ARRAY(
                   SELECT a.attname::pg_catalog.text
                   FROM pg_catalog.unnest(i.indkey::pg_catalog.int2[])
                        WITH ORDINALITY AS k(attnum, key_position)
                   JOIN pg_catalog.pg_attribute a
                     ON a.attrelid OPERATOR(pg_catalog.=) i.indrelid
                    AND a.attnum OPERATOR(pg_catalog.=) k.attnum
                   WHERE k.key_position OPERATOR(pg_catalog.<=) i.indnkeyatts
                   ORDER BY k.key_position
                 ) AS column_names
          FROM visible_tables vt
          JOIN pg_catalog.pg_index i ON i.indrelid OPERATOR(pg_catalog.=) vt.oid
          JOIN pg_catalog.pg_class ic ON ic.oid OPERATOR(pg_catalog.=) i.indexrelid
          WHERE i.indisunique
            AND i.indisvalid
            AND i.indpred IS NULL
            AND 0 OPERATOR(pg_catalog.<>) ALL (i.indkey::pg_catalog.int2[])
            AND NOT EXISTS (
              SELECT 1
              FROM pg_catalog.pg_constraint con
              WHERE con.conindid OPERATOR(pg_catalog.=) i.indexrelid
                AND con.conrelid OPERATOR(pg_catalog.=) i.indrelid
                AND con.contype OPERATOR(pg_catalog.=) ANY ('{p,u}'::pg_catalog."char"[])
            )
          """;

  /** Enum types in requested schemas the role has {@code USAGE} on, labels by sort order. */
  private static final String ENUMS =
      """
      SELECT n.nspname AS schema_name,
             t.typname AS enum_name,
             ARRAY(
               SELECT e.enumlabel::pg_catalog.text
               FROM pg_catalog.pg_enum e
               WHERE e.enumtypid OPERATOR(pg_catalog.=) t.oid
               ORDER BY e.enumsortorder
             ) AS labels
      FROM pg_catalog.pg_type t
      JOIN pg_catalog.pg_namespace n ON n.oid OPERATOR(pg_catalog.=) t.typnamespace
      WHERE t.typtype OPERATOR(pg_catalog.=) 'e'
        AND n.nspname OPERATOR(pg_catalog.=) ANY (?)
        AND pg_catalog.has_schema_privilege(n.oid, 'USAGE')
      """;

  /**
   * Runs the six statements, binding {@code schemaNames} to each, and collects their rows. The
   * statements are used only here, so their text exists in this class file alone.
   */
  static PgCatalogRows readRows(
      final Connection connection, final Array schemaNames, final List<String> requestedSchemas)
      throws SQLException {
    final PgCatalogRows.Builder rows = PgCatalogRows.builder().requestedSchemas(requestedSchemas);
    try (PreparedStatement statement = connection.prepareStatement(SCHEMAS)) {
      final List<SchemaRow> schemas = executeQuery(statement, schemaNames, SchemaRow::map);
      rows.schemaNames(schemas.stream().filter(SchemaRow::hasUsage).map(SchemaRow::name).toList())
          .schemasWithoutUsage(
              schemas.stream().filter(schema -> !schema.hasUsage()).map(SchemaRow::name).toList());
    }
    try (PreparedStatement statement = connection.prepareStatement(TABLES)) {
      rows.tables(executeQuery(statement, schemaNames, PgCatalogRowMappers::mapTable));
    }
    try (PreparedStatement statement = connection.prepareStatement(COLUMNS)) {
      rows.columns(executeQuery(statement, schemaNames, PgCatalogRowMappers::mapColumn));
    }
    try (PreparedStatement statement = connection.prepareStatement(CONSTRAINTS)) {
      rows.constraints(executeQuery(statement, schemaNames, PgCatalogRowMappers::mapConstraint));
    }
    try (PreparedStatement statement = connection.prepareStatement(UNIQUE_INDEXES)) {
      rows.uniqueIndexes(executeQuery(statement, schemaNames, PgCatalogRowMappers::mapUniqueIndex));
    }
    try (PreparedStatement statement = connection.prepareStatement(ENUMS)) {
      rows.enums(executeQuery(statement, schemaNames, PgCatalogRowMappers::mapEnum));
    }
    return rows.build();
  }

  /** Binds the schema names to the statement's single parameter and maps every result row. */
  private static <T> List<T> executeQuery(
      final PreparedStatement statement, final Array schemaNames, final RowMapper<T> mapper)
      throws SQLException {
    statement.setArray(1, schemaNames);
    try (ResultSet resultSet = statement.executeQuery()) {
      final List<T> rows = new ArrayList<>();
      while (resultSet.next()) {
        rows.add(mapper.map(resultSet));
      }
      return rows;
    }
  }

  /** A {@link #SCHEMAS} row, split into the two schema lists of the rows. */
  private record SchemaRow(String name, boolean hasUsage) {

    private static SchemaRow map(final ResultSet resultSet) throws SQLException {
      return new SchemaRow(resultSet.getString("schema_name"), resultSet.getBoolean("has_usage"));
    }
  }

  private PgCatalogQueries() {
    throw new IllegalStateException("Utility class");
  }
}
