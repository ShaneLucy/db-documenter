package db.documenter.internal.queries;

import db.documenter.internal.queries.api.PreparedStatements;
import db.documenter.internal.queries.impl.postgresql.PostgresqlPreparedStatements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Sealed abstract base class that implements the subset of {@link PreparedStatements} methods whose SQL is
 * portable across ANSI SQL-compatible engines.
 *
 * <p>The {@code sealed} modifier restricts the class hierarchy to known implementations, enabling
 * exhaustive pattern matching and preventing unexpected extension.
 *
 * @see PreparedStatements
 * @see PostgresqlPreparedStatements
 */
public abstract sealed class SqlAgnosticPreparedStatements implements PreparedStatements permits PostgresqlPreparedStatements {

  private static final String COLUMN_INFO_QUERY =
      """
           SELECT
               c.column_name,
               c.ordinal_position,
               c.is_nullable,
               c.data_type,
               c.character_maximum_length,
               c.numeric_precision,
               c.numeric_scale,
               c.column_default,
               c.is_generated,
               c.generation_expression,
               CASE WHEN EXISTS (
                   SELECT 1
                   FROM information_schema.key_column_usage      kcu2
                   JOIN information_schema.table_constraints     tc2
                     ON tc2.constraint_name = kcu2.constraint_name
                    AND tc2.table_schema    = kcu2.table_schema
                    AND tc2.constraint_type = 'UNIQUE'
                   WHERE kcu2.table_schema = c.table_schema
                     AND kcu2.table_name   = c.table_name
                     AND kcu2.column_name  = c.column_name
               ) THEN true ELSE false END                        AS is_unique,
               NULL                                              AS composite_unique_constraint_name,
               CASE WHEN EXISTS (
                   SELECT 1
                   FROM information_schema.constraint_column_usage ccu2
                   JOIN information_schema.check_constraints       cc2
                     ON cc2.constraint_name = ccu2.constraint_name
                   JOIN information_schema.table_constraints       tc2
                     ON tc2.constraint_name = cc2.constraint_name
                    AND tc2.constraint_type = 'CHECK'
                   WHERE ccu2.table_schema = c.table_schema
                     AND ccu2.table_name   = c.table_name
                     AND ccu2.column_name  = c.column_name
               ) THEN 'HAS_CHECK' ELSE NULL END                  AS check_constraint,
               false                                             AS is_auto_increment
           FROM information_schema.columns c
           WHERE c.table_schema = ?
             AND c.table_name   = ?
           ORDER BY c.ordinal_position;
           """;

  private static final String TABLE_INFO_QUERY =
      """
           SELECT
             table_name,
             NULL AS partition_key
           FROM information_schema.tables
           WHERE table_schema = ?
             AND table_type = 'BASE TABLE'
           ORDER BY table_name;
           """;

  private static final String PRIMARY_KEY_INFO_QUERY =
      """
            SELECT
                tc.constraint_name,
                tc.table_name,
                kcu.column_name
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            WHERE tc.constraint_type = 'PRIMARY KEY'
                AND tc.table_schema = ?
                AND tc.table_name = ?;
            """;

  private static final String VIEW_INFO_QUERY =
      """
           SELECT
             v.table_name
           FROM information_schema.views v
           WHERE v.table_schema = ?
           ORDER BY v.table_name;
           """;

  private static final String FOREIGN_KEY_INFO_QUERY =
      """
            SELECT
                tc.constraint_name,
                kcu.table_schema           AS source_schema,
                kcu.table_name             AS source_table_name,
                kcu.column_name            AS source_column,
                ccu.table_schema           AS referenced_schema,
                ccu.table_name             AS referenced_table,
                ccu.column_name            AS referenced_column,
                rc.delete_rule             AS on_delete_type,
                rc.update_rule             AS on_update_type
            FROM information_schema.table_constraints       AS tc
            JOIN information_schema.key_column_usage        AS kcu
              ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
              ON tc.constraint_name = ccu.constraint_name
            JOIN information_schema.referential_constraints AS rc
              ON tc.constraint_name = rc.constraint_name AND tc.table_schema = rc.constraint_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND kcu.table_schema = ? AND kcu.table_name = ?;
            """;

  public PreparedStatement tableInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(TABLE_INFO_QUERY);
  }

  public PreparedStatement columnInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(COLUMN_INFO_QUERY);
  }

  public PreparedStatement primaryKeyInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(PRIMARY_KEY_INFO_QUERY);
  }

  public PreparedStatement foreignKeyInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(FOREIGN_KEY_INFO_QUERY);
  }

  public PreparedStatement viewInfoPreparedStatement(final Connection connection)
      throws SQLException {
    return connection.prepareStatement(VIEW_INFO_QUERY);
  }
}
