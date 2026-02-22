package db.documenter.internal.queries.impl.postgresql;

import db.documenter.internal.queries.SqlAgnosticPreparedStatements;
import db.documenter.internal.queries.api.PreparedStatements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PostgreSQL-specific implementation of {@link PreparedStatements}.
 *
 * <p>Provides SQL strings for queries that target PostgreSQL-specific system catalogs ({@code
 * pg_catalog}, {@code pg_type}, {@code pg_enum}, {@code pg_class}, {@code pg_namespace}, etc.) and
 * PostgreSQL extensions to {@code information_schema}. Queries portable across ANSI SQL-compatible
 * engines ({@code primaryKeyInfoQuery}, {@code viewInfoQuery}, {@code foreignKeyInfoQuery}) are
 * inherited from {@link SqlAgnosticPreparedStatements}. The {@code tableInfoQuery} override retained here
 * augments the ANSI base with partition filtering ({@code relispartition = false}) and partition
 * key derivation ({@code pg_get_partkeydef}) that have no ANSI equivalent.
 *
 * @see SqlAgnosticPreparedStatements
 */
public final class PostgresqlPreparedStatements extends SqlAgnosticPreparedStatements {

  private static final String GET_TABLE_INFO_QUERY =
      """
                 SELECT
                   t.table_name,
                   CASE WHEN c.relkind = 'p' THEN pg_get_partkeydef(c.oid) ELSE NULL END AS partition_key
                 FROM information_schema.tables t
                 JOIN pg_catalog.pg_class c ON c.relname = t.table_name
                 JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace AND n.nspname = t.table_schema
                 WHERE t.table_schema = ?
                   AND t.table_type = 'BASE TABLE'
                   AND c.relispartition = false
                 ORDER BY c.oid;
                 """;
  private static final String GET_COLUMN_INFO_QUERY =
      """
           SELECT DISTINCT
             c.column_name,
             c.ordinal_position,
             c.is_nullable,
             CASE
               WHEN c.data_type = 'ARRAY' THEN SUBSTRING(c.udt_name FROM 2) || '[]'
               ELSE c.data_type
             END AS data_type,
             c.udt_schema,
             c.character_maximum_length,
             c.numeric_precision,
             c.numeric_scale,
             c.column_default,
             c.is_generated,
             c.generation_expression,
             CASE
               WHEN COUNT(DISTINCT uc.constraint_name) > 0 THEN true
               ELSE false
             END AS is_unique,
             (
               SELECT STRING_AGG(DISTINCT uc2.constraint_name, ',')
               FROM information_schema.table_constraints uc2
               JOIN information_schema.key_column_usage kcu2
                 ON uc2.constraint_name = kcu2.constraint_name
                 AND uc2.table_schema = kcu2.table_schema
               WHERE uc2.constraint_type = 'UNIQUE'
                 AND uc2.table_schema = c.table_schema
                 AND uc2.table_name = c.table_name
                 AND kcu2.column_name = c.column_name
                 AND (
                   SELECT COUNT(*)
                   FROM information_schema.key_column_usage kcu3
                   WHERE kcu3.constraint_name = uc2.constraint_name
                     AND kcu3.table_schema = uc2.table_schema
                 ) > 1
             ) AS composite_unique_constraint_name,
             STRING_AGG(DISTINCT cc.check_clause, ' AND ') AS check_constraint,
             CASE
               WHEN c.column_default LIKE 'nextval%' THEN true
               ELSE false
             END AS is_auto_increment
           FROM information_schema.columns c
           LEFT JOIN information_schema.key_column_usage kcu
             ON c.table_schema = kcu.table_schema
             AND c.table_name = kcu.table_name
             AND c.column_name = kcu.column_name
           LEFT JOIN information_schema.table_constraints uc
             ON kcu.constraint_name = uc.constraint_name
             AND kcu.table_schema = uc.table_schema
             AND uc.constraint_type = 'UNIQUE'
           LEFT JOIN information_schema.constraint_column_usage ccu
             ON c.table_schema = ccu.table_schema
             AND c.table_name = ccu.table_name
             AND c.column_name = ccu.column_name
           LEFT JOIN information_schema.check_constraints cc
             ON ccu.constraint_name = cc.constraint_name
             AND EXISTS (
               SELECT 1 FROM information_schema.table_constraints tc
               WHERE tc.constraint_name = cc.constraint_name
               AND tc.constraint_type = 'CHECK'
             )
           WHERE c.table_schema = ?
             AND c.table_name = ?
           GROUP BY c.table_schema, c.table_name, c.column_name, c.ordinal_position,
                    c.is_nullable, c.data_type,
                    c.udt_name, c.udt_schema, c.character_maximum_length,
                    c.numeric_precision, c.numeric_scale,
                    c.column_default, c.is_generated, c.generation_expression
           ORDER BY c.ordinal_position;
           """;

  private static final String GET_ENUMS_QUERY =
      """
          SELECT DISTINCT
            n.nspname AS udt_schema,
            t.typname AS udt_name
          FROM pg_type t
          JOIN pg_namespace n ON t.typnamespace = n.oid
          WHERE n.nspname = ?
            AND t.typtype = 'e'
          ORDER BY t.typname;
          """;

  private static final String GET_ENUM_FIELDS_QUERY =
      """
          SELECT
              e.enumlabel
          FROM pg_type t
          JOIN pg_enum e ON t.oid = e.enumtypid
          JOIN pg_namespace n ON n.oid = t.typnamespace
          WHERE n.nspname = ?
            AND t.typname = ?
          ORDER BY e.enumsortorder;
          """;

  private static final String GET_COLUMN_ENUM_MAPPINGS_QUERY =
      """
          SELECT
            c.table_name,
            c.column_name,
            c.udt_schema,
            c.udt_name
          FROM information_schema.columns c
          WHERE c.table_schema = ?
            AND c.data_type = 'USER-DEFINED'
          ORDER BY c.table_name, c.ordinal_position;
          """;

  private static final String GET_COMPOSITE_TYPES_QUERY =
      """
          SELECT
              t.typname AS type_name,
              n.nspname AS schema_name,
              a.attname AS attribute_name,
              pg_catalog.format_type(a.atttypid, a.atttypmod) AS attribute_type,
              a.attnum AS attribute_position
          FROM pg_type t
          JOIN pg_namespace n ON n.oid = t.typnamespace
          LEFT JOIN pg_attribute a ON a.attrelid = t.typrelid AND a.attnum > 0 AND NOT a.attisdropped
          WHERE n.nspname = ?
            AND t.typtype = 'c'
            AND t.typrelid != 0
            AND NOT EXISTS (
                SELECT 1 FROM pg_class c
                WHERE c.oid = t.typrelid AND c.relkind IN ('r', 'v', 'm', 'p')
            )
          ORDER BY t.typname, a.attnum;
          """;

  private static final String GET_MATERIALIZED_VIEWS_QUERY =
      """
           SELECT
             c.relname AS table_name
           FROM pg_catalog.pg_class c
           JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
           WHERE n.nspname = ?
             AND c.relkind = 'm'
           ORDER BY c.relname;
           """;

  private static final String GET_MATERIALIZED_VIEW_COLUMN_INFO_QUERY =
      """
           SELECT
             a.attname AS column_name,
             a.attnum AS ordinal_position,
             CASE WHEN a.attnotnull THEN 'NO' ELSE 'YES' END AS is_nullable,
             CASE
               WHEN t.typcategory = 'A' THEN
                 CASE
                   WHEN et.typname = 'int2'        THEN 'smallint'
                   WHEN et.typname = 'int4'        THEN 'integer'
                   WHEN et.typname = 'int8'        THEN 'bigint'
                   WHEN et.typname = 'varchar'     THEN 'character varying'
                   WHEN et.typname = 'bpchar'      THEN 'character'
                   WHEN et.typname = 'bool'        THEN 'boolean'
                   WHEN et.typname = 'float4'      THEN 'real'
                   WHEN et.typname = 'float8'      THEN 'double precision'
                   WHEN et.typname = 'timestamptz' THEN 'timestamp with time zone'
                   WHEN et.typname = 'timestamp'   THEN 'timestamp without time zone'
                   ELSE et.typname
                 END || '[]'
               WHEN t.typtype IN ('e', 'c') THEN 'USER-DEFINED'
               WHEN t.typname = 'int2'        THEN 'smallint'
               WHEN t.typname = 'int4'        THEN 'integer'
               WHEN t.typname = 'int8'        THEN 'bigint'
               WHEN t.typname = 'varchar'     THEN 'character varying'
               WHEN t.typname = 'bpchar'      THEN 'character'
               WHEN t.typname = 'bool'        THEN 'boolean'
               WHEN t.typname = 'float4'      THEN 'real'
               WHEN t.typname = 'float8'      THEN 'double precision'
               WHEN t.typname = 'timestamptz' THEN 'timestamp with time zone'
               WHEN t.typname = 'timestamp'   THEN 'timestamp without time zone'
               ELSE t.typname
             END AS data_type,
             tn.nspname AS udt_schema,
             CASE
               WHEN t.typname IN ('varchar', 'bpchar') AND a.atttypmod > 0
                 THEN (a.atttypmod - 4)::integer
               WHEN t.typcategory = 'A'
                 AND et.typname IN ('varchar', 'bpchar') AND a.atttypmod > 0
                 THEN (a.atttypmod - 4)::integer
               ELSE 0
             END AS character_maximum_length,
             CASE WHEN t.typname = 'numeric' AND a.atttypmod > 0
               THEN (((a.atttypmod - 4) >> 16) & 65535)::integer
               ELSE NULL
             END AS numeric_precision,
             CASE WHEN t.typname = 'numeric' AND a.atttypmod > 0
               THEN ((a.atttypmod - 4) & 65535)::integer
               ELSE NULL
             END AS numeric_scale,
             NULL::text AS column_default,
             'NEVER'::text AS is_generated,
             NULL::text AS generation_expression,
             false AS is_unique,
             NULL::text AS composite_unique_constraint_name,
             NULL::text AS check_constraint,
             false AS is_auto_increment
           FROM pg_catalog.pg_attribute a
           JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
           JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
           JOIN pg_catalog.pg_type t ON t.oid = a.atttypid
           JOIN pg_catalog.pg_namespace tn ON tn.oid = t.typnamespace
           LEFT JOIN pg_catalog.pg_type et ON et.oid = t.typelem
           WHERE n.nspname = ?
             AND c.relname = ?
             AND c.relkind = 'm'
             AND a.attnum > 0
             AND NOT a.attisdropped
           ORDER BY a.attnum;
           """;

  private static final String GET_PARTITION_CHILDREN_QUERY =
      """
           SELECT
               p.relname AS table_name,
               c.relname AS partition_name
           FROM pg_class p
           JOIN pg_namespace n ON n.oid = p.relnamespace
           JOIN pg_inherits i ON i.inhparent = p.oid
           JOIN pg_class c ON c.oid = i.inhrelid
           WHERE n.nspname = ?
             AND p.relkind = 'p'
           ORDER BY p.relname, c.relname;
           """;

  @Override
  public PreparedStatement tableInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_TABLE_INFO_QUERY);
  }

  @Override
  public PreparedStatement columnInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_COLUMN_INFO_QUERY);
  }

  public PreparedStatement enumInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_ENUMS_QUERY);
  }

  public PreparedStatement enumValuesPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_ENUM_FIELDS_QUERY);
  }

  public PreparedStatement columnUdtMappingsPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_COLUMN_ENUM_MAPPINGS_QUERY);
  }

  public PreparedStatement compositeTypeInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_COMPOSITE_TYPES_QUERY);
  }

  public PreparedStatement materializedViewInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_MATERIALIZED_VIEWS_QUERY);
  }

  public PreparedStatement materializedViewColumnInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_MATERIALIZED_VIEW_COLUMN_INFO_QUERY);
  }

  public PreparedStatement partitionChildrenPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_PARTITION_CHILDREN_QUERY);
  }
}
