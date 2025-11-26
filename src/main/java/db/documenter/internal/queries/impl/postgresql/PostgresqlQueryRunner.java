package db.documenter.internal.queries.impl.postgresql;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.api.PreparedStatementMapper;
import db.documenter.internal.queries.api.QueryRunner;
import db.documenter.internal.queries.api.ResultSetMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PostgresqlQueryRunner implements QueryRunner {

  private final PreparedStatementMapper postgresqlPreparedStatementMapper;
  private final ResultSetMapper postgresqlResultSetMapper;
  private final ConnectionHolder connectionHolder;

  private static final Logger LOGGER = Logger.getLogger(PostgresqlQueryRunner.class.getName());

  private static final String GET_TABLE_INFO_QUERY =
      """
           SELECT
             table_name
           FROM information_schema.tables
           WHERE table_schema = ?;
           """;

  private static final String GET_COLUMN_INFO_QUERY =
      """
           SELECT
             column_name,
             ordinal_position,
             is_nullable,
             data_type,
             character_maximum_length
           FROM information_schema.columns
           WHERE table_schema = ?
             AND table_name = ?;
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
                tc.table_name AS source_table_name,
                kcu.column_name AS source_column,
                ccu.table_name AS referenced_table,
                ccu.column_name AS referenced_column
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
              ON ccu.constraint_name = tc.constraint_name
              AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND tc.table_schema = ? AND tc.table_name = ?;
            """;

  public PostgresqlQueryRunner(
      final PreparedStatementMapper postgresqlPreparedStatementMapper,
      final ResultSetMapper postgresqlResultSetMapper,
      final Connection connection) {
    this.postgresqlPreparedStatementMapper = postgresqlPreparedStatementMapper;
    this.postgresqlResultSetMapper = postgresqlResultSetMapper;
    this.connectionHolder = new ConnectionHolder(connection);
  }

  @Override
  public List<Table> getTableInfo(final String schema) throws SQLException {
    try (final var preparedStatement =
        connectionHolder.connection().prepareStatement(GET_TABLE_INFO_QUERY)) {
      postgresqlPreparedStatementMapper.prepareTableInfoStatement(preparedStatement, schema);

      final var resultSet = preparedStatement.executeQuery();
      final List<Table> tables = postgresqlResultSetMapper.mapToTables(resultSet);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.log(
            Level.INFO,
            "Discovered: {0} tables in schema: {1}",
            new Object[] {tables.size(), schema});
      }
      return tables;
    }
  }

  @Override
  public List<Column> getColumnInfo(final String schema, final Table table) throws SQLException {
    try (final var preparedStatement =
        connectionHolder.connection().prepareStatement(GET_COLUMN_INFO_QUERY)) {
      postgresqlPreparedStatementMapper.prepareColumnInfoStatement(
          preparedStatement, schema, table.name());

      final var resultSet = preparedStatement.executeQuery();
      final List<Column> columns = postgresqlResultSetMapper.mapToColumns(resultSet);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.log(
            Level.INFO,
            "Discovered: {0} columns for table: {1} in schema: {2}",
            new Object[] {columns.size(), table.name(), schema});
      }
      return columns;
    }
  }

  @Override
  public PrimaryKey getPrimaryKeyInfo(final String schema, final Table table) throws SQLException {
    try (final var preparedStatement =
        connectionHolder.connection().prepareStatement(GET_PRIMARY_KEY_INFO_QUERY)) {
      postgresqlPreparedStatementMapper.preparePrimaryKeyInfoStatement(
          preparedStatement, schema, table);

      final var resultSet = preparedStatement.executeQuery();

      return postgresqlResultSetMapper.mapToPrimaryKey(resultSet);
    }
  }

  @Override
  public List<ForeignKey> getForeignKeyInfo(final String schema, final Table table)
      throws SQLException {
    try (final var preparedStatement =
        connectionHolder.connection().prepareStatement(GET_FOREIGN_KEY_INFO)) {
      postgresqlPreparedStatementMapper.prepareForeignKeyInfoStatement(
          preparedStatement, schema, table);

      final var resultSet = preparedStatement.executeQuery();

      final List<ForeignKey> foreignKeys = postgresqlResultSetMapper.mapToForeignKeys(resultSet);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.log(
            Level.INFO,
            "Discovered: {0} foreign keys for table: {1} in schema: {2}",
            new Object[] {foreignKeys.size(), table.name(), schema});
      }
      return foreignKeys;
    }
  }

  private record ConnectionHolder(Connection connection) implements AutoCloseable {

    @Override
    public void close() throws SQLException {
      if (connection != null && !connection().isClosed()) {
        connection().close();
      }
    }
  }
}
