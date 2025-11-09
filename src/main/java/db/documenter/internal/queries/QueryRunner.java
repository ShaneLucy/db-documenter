package db.documenter.internal.queries;

import db.documenter.internal.db.api.ConnectionManager;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.preparedstatements.PreparedStatementMapper;
import db.documenter.internal.queries.resultsets.ResultSetMapper;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueryRunner {

  private final PreparedStatementMapper preparedStatementMapper;
  private final ResultSetMapper resultSetMapper;
  private final ConnectionManager connectionManager;

  private static final Logger LOGGER = Logger.getLogger(QueryRunner.class.getName());

  private static final String GET_TABLE_INFO_QUERY =
      "SELECT table_schema, table_name, table_type FROM information_schema.tables WHERE table_schema = ?;";
  private static final String GET_COLUMN_INFO_QUERY =
      "SELECT * FROM information_schema.columns WHERE table_schema = ? AND table_name = ?;";
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

  public QueryRunner(
      final PreparedStatementMapper preparedStatementMapper,
      final ResultSetMapper resultSetMapper,
      final ConnectionManager connectionManager) {
    this.preparedStatementMapper = preparedStatementMapper;
    this.resultSetMapper = resultSetMapper;
    this.connectionManager = connectionManager;
  }

  public List<Table> getTableInfo(final String schema) throws SQLException {
    try (final var connection = connectionManager.getConnection()) {
      try (final var preparedStatement = connection.prepareStatement(GET_TABLE_INFO_QUERY)) {
        preparedStatementMapper.prepareTableInfoStatement(preparedStatement, schema);
        final var resultSet = preparedStatement.executeQuery();
        final List<Table> tables = new ArrayList<>();
        while (resultSet.next()) {
          tables.add(resultSetMapper.mapToTable(resultSet));
        }

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} tables in schema: {1}",
              new Object[] {tables.size(), schema});
        }
        return tables;
      }
    }
  }

  public List<Column> getColumnInfo(final String schema, final Table table) throws SQLException {
    try (final var connection = connectionManager.getConnection()) {
      try (final var preparedStatement = connection.prepareStatement(GET_COLUMN_INFO_QUERY)) {
        preparedStatementMapper.prepareColumnInfoStatement(preparedStatement, schema, table.name());

        final var resultSet = preparedStatement.executeQuery();

        final List<Column> columns = new ArrayList<>();
        while (resultSet.next()) {
          columns.add(resultSetMapper.mapToColumn(resultSet));
        }

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} columns for table: {1} in schema: {2}",
              new Object[] {columns.size(), table.name(), schema});
        }
        return columns;
      }
    }
  }

  public PrimaryKey getPrimaryKeyInfo(final String schema, final Table table) throws SQLException {
    try (final var connection = connectionManager.getConnection()) {
      try (final var preparedStatement = connection.prepareStatement(GET_PRIMARY_KEY_INFO_QUERY)) {
        preparedStatementMapper.preparePrimaryKeyInfoStatement(preparedStatement, schema, table);

        final var resultSet = preparedStatement.executeQuery();

        return resultSetMapper.mapToPrimaryKey(resultSet);
      }
    }
  }

  public List<ForeignKey> getForeignKeyInfo(final String schema, final Table table)
      throws SQLException {
    try (final var connection = connectionManager.getConnection()) {
      try (final var preparedStatement = connection.prepareStatement(GET_FOREIGN_KEY_INFO)) {
        preparedStatementMapper.prepareForeignKeyInfoStatement(preparedStatement, schema, table);

        final var resultSet = preparedStatement.executeQuery();

        final List<ForeignKey> foreignKeys = new ArrayList<>();
        while (resultSet.next()) {
          foreignKeys.add(resultSetMapper.mapToForeignKey(resultSet));
        }

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} foreign keys for table: {1} in schema: {2}",
              new Object[] {foreignKeys.size(), table.name(), schema});
        }
        return foreignKeys;
      }
    }
  }
}
