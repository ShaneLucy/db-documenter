package db.documenter.internal.queries.impl.postgresql;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.api.PreparedStatementMapper;
import db.documenter.internal.queries.api.QueryRunner;
import db.documenter.internal.queries.api.ResultSetMapper;
import db.documenter.internal.utils.LogUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PostgresqlQueryRunner implements QueryRunner {

  private final PreparedStatementMapper postgresqlPreparedStatementMapper;
  private final ResultSetMapper postgresqlResultSetMapper;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Connection lifecycle managed by caller (SchemaBuilder) via try-with-resources. Field is final and private.")
  private final Connection connection;

  private static final Logger LOGGER = Logger.getLogger(PostgresqlQueryRunner.class.getName());

  private static final String GET_TABLE_INFO_QUERY =
      """
           SELECT
             t.table_name
           FROM information_schema.tables t
           JOIN pg_catalog.pg_class c ON c.relname = t.table_name
           JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace AND n.nspname = t.table_schema
           WHERE t.table_schema = ?
           ORDER BY c.oid;
           """;

  private static final String GET_COLUMN_INFO_QUERY =
      """
           SELECT DISTINCT
             c.column_name,
             c.ordinal_position,
             c.is_nullable,
             c.data_type,
             c.udt_schema,
             c.character_maximum_length,
             c.column_default,
             CASE
               WHEN COUNT(DISTINCT uc.constraint_name) > 0 THEN true
               ELSE false
             END AS is_unique,
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
           GROUP BY c.column_name, c.ordinal_position, c.is_nullable, c.data_type,
                    c.udt_schema, c.character_maximum_length, c.column_default
           ORDER BY c.ordinal_position;
           """;

  private static final String GET_PRIMARY_KEY_INFO_QUERY =
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

  private static final String GET_FOREIGN_KEY_INFO =
      """
            SELECT
                tc.constraint_name,
                kcu.table_schema AS source_schema,
                kcu.table_name AS source_table_name,
                kcu.column_name AS source_column,
                ccu.table_schema AS referenced_schema,
                ccu.table_name AS referenced_table,
                ccu.column_name AS referenced_column
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
              ON tc.constraint_name = ccu.constraint_name
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND kcu.table_schema = ? AND kcu.table_name = ?;
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

  public PostgresqlQueryRunner(
      final PreparedStatementMapper postgresqlPreparedStatementMapper,
      final ResultSetMapper postgresqlResultSetMapper,
      final Connection connection) {
    this.postgresqlPreparedStatementMapper = postgresqlPreparedStatementMapper;
    this.postgresqlResultSetMapper = postgresqlResultSetMapper;
    this.connection = connection;
  }

  @Override
  public List<Table> getTableInfo(final String schema) throws SQLException {
    try (final var preparedStatement = connection.prepareStatement(GET_TABLE_INFO_QUERY)) {
      postgresqlPreparedStatementMapper.prepareTableInfoStatement(preparedStatement, schema);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<Table> tables = postgresqlResultSetMapper.mapToTables(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} tables in schema: {1}",
              new Object[] {tables.size(), LogUtils.sanitizeForLog(schema)});
        }
        return tables;
      }
    }
  }

  @Override
  public List<Column> getColumnInfo(final String schema, final Table table) throws SQLException {
    try (final var preparedStatement = connection.prepareStatement(GET_COLUMN_INFO_QUERY)) {
      postgresqlPreparedStatementMapper.prepareColumnInfoStatement(
          preparedStatement, schema, table.name());

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<Column> columns = postgresqlResultSetMapper.mapToColumns(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} columns for table: {1} in schema: {2}",
              new Object[] {
                columns.size(),
                LogUtils.sanitizeForLog(table.name()),
                LogUtils.sanitizeForLog(schema)
              });
        }
        return columns;
      }
    }
  }

  @Override
  public PrimaryKey getPrimaryKeyInfo(final String schema, final Table table) throws SQLException {
    try (final var preparedStatement = connection.prepareStatement(GET_PRIMARY_KEY_INFO_QUERY)) {
      postgresqlPreparedStatementMapper.preparePrimaryKeyInfoStatement(
          preparedStatement, schema, table);

      try (final var resultSet = preparedStatement.executeQuery()) {
        return postgresqlResultSetMapper.mapToPrimaryKey(resultSet);
      }
    }
  }

  @Override
  public List<ForeignKey> getForeignKeyInfo(final String schema, final Table table)
      throws SQLException {
    try (final var preparedStatement = connection.prepareStatement(GET_FOREIGN_KEY_INFO)) {
      postgresqlPreparedStatementMapper.prepareForeignKeyInfoStatement(
          preparedStatement, schema, table);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<ForeignKey> foreignKeys = postgresqlResultSetMapper.mapToForeignKeys(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} foreign keys for table: {1} in schema: {2}",
              new Object[] {
                foreignKeys.size(),
                LogUtils.sanitizeForLog(table.name()),
                LogUtils.sanitizeForLog(schema)
              });
        }
        return foreignKeys;
      }
    }
  }

  @Override
  public List<DbEnum> getEnumInfo(final String schema) throws SQLException {
    try (final var preparedStatement = connection.prepareStatement(GET_ENUMS_QUERY)) {
      postgresqlPreparedStatementMapper.prepareEnumInfoStatement(preparedStatement, schema);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<DbEnum> dbEnums = postgresqlResultSetMapper.mapToDbEnumInfo(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} enums in schema: {1}",
              new Object[] {dbEnums.size(), LogUtils.sanitizeForLog(schema)});
        }

        return dbEnums;
      }
    }
  }

  @Override
  public List<String> getEnumValues(final String schema, final DbEnum dbEnum) throws SQLException {
    try (final var preparedStatement = connection.prepareStatement(GET_ENUM_FIELDS_QUERY)) {
      postgresqlPreparedStatementMapper.prepareEnumValuesStatement(
          preparedStatement, schema, dbEnum.enumName());

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<String> dbEnumValues = postgresqlResultSetMapper.mapToDbEnumValues(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} values for enum {1} in schema: {2}",
              new Object[] {
                dbEnumValues.size(),
                LogUtils.sanitizeForLog(dbEnum.enumName()),
                LogUtils.sanitizeForLog(schema)
              });
        }

        return dbEnumValues;
      }
    }
  }

  @Override
  public Map<ColumnKey, UdtReference> getColumnUdtMappings(final String schemaName)
      throws SQLException {
    try (final var preparedStatement =
        connection.prepareStatement(GET_COLUMN_ENUM_MAPPINGS_QUERY)) {
      postgresqlPreparedStatementMapper.prepareColumnUdtMappingsStatement(
          preparedStatement, schemaName);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final Map<ColumnKey, UdtReference> columnUdtMappings =
            postgresqlResultSetMapper.mapToColumnUdtMappings(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} column UDT mappings in schema: {1}",
              new Object[] {columnUdtMappings.size(), LogUtils.sanitizeForLog(schemaName)});
        }

        return columnUdtMappings;
      }
    }
  }
}
