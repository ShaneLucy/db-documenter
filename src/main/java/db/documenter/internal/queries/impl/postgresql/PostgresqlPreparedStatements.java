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
 * engines ({@code primaryKeyInfoQuery}, {@code foreignKeyInfoQuery}) are inherited from {@link
 * SqlAgnosticPreparedStatements}. The {@code tableInfoQuery} override retained here augments the
 * ANSI base with partition filtering ({@code relispartition = false}) and partition key derivation
 * ({@code pg_get_partkeydef}) that have no ANSI equivalent.
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
             CASE
               WHEN COUNT(DISTINCT uc.constraint_name) > 0 THEN true
               ELSE false
             END AS is_unique
           FROM information_schema.columns c
           LEFT JOIN information_schema.key_column_usage kcu
             ON c.table_schema = kcu.table_schema
             AND c.table_name = kcu.table_name
             AND c.column_name = kcu.column_name
           LEFT JOIN information_schema.table_constraints uc
             ON kcu.constraint_name = uc.constraint_name
             AND kcu.table_schema = uc.table_schema
             AND uc.constraint_type = 'UNIQUE'
           WHERE c.table_schema = ?
             AND c.table_name = ?
           GROUP BY c.table_schema, c.table_name, c.column_name, c.ordinal_position,
                    c.is_nullable, c.data_type,
                    c.udt_name, c.udt_schema, c.character_maximum_length,
                    c.numeric_precision, c.numeric_scale
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

  public PreparedStatement partitionChildrenPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(GET_PARTITION_CHILDREN_QUERY);
  }
}
