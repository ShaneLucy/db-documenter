package dev.shanelucy.dbdocumenter.internal.queries;

import dev.shanelucy.dbdocumenter.internal.models.db.Column;
import dev.shanelucy.dbdocumenter.internal.models.db.ForeignKey;
import dev.shanelucy.dbdocumenter.internal.models.db.PrimaryKey;
import dev.shanelucy.dbdocumenter.internal.models.db.Table;
import dev.shanelucy.dbdocumenter.internal.queries.api.PreparedStatementMapper;
import dev.shanelucy.dbdocumenter.internal.queries.api.PreparedStatements;
import dev.shanelucy.dbdocumenter.internal.queries.api.QueryRunner;
import dev.shanelucy.dbdocumenter.internal.queries.api.ResultSetMapper;
import dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import dev.shanelucy.dbdocumenter.internal.utils.LogUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sealed base class for all database-specific query runner implementations.
 *
 * <p>Provides concrete implementations for the four generic methods in the {@link QueryRunner}
 * interface by combining a {@link PreparedStatements} SQL source, a {@link PreparedStatementMapper}
 * for parameter binding, a {@link ResultSetMapper} for result mapping, and a JDBC {@link
 * Connection}. The {@code sealed} modifier restricts the class hierarchy to known implementations,
 * enabling exhaustive pattern matching and preventing unexpected extension.
 *
 * <p>Engine-specific operations that extend beyond the four generic methods (e.g., PostgreSQL enum
 * queries, UDT mappings) are declared directly on the permitted concrete subclass rather than on
 * this abstract class or the interface.
 *
 * <p><b>Resource Management:</b> All {@link java.sql.PreparedStatement} and {@link
 * java.sql.ResultSet} instances are managed with try-with-resources to guarantee closure even on
 * exceptions. The {@link Connection} lifecycle is owned by the caller (typically {@link
 * dev.shanelucy.dbdocumenter.internal.builder.PostgresqlSchemaBuilder}).
 *
 * @see QueryRunner
 * @see PostgresqlQueryRunner
 */
public abstract sealed class AbstractQueryRunner implements QueryRunner
    permits PostgresqlQueryRunner {

  private static final Logger LOGGER = Logger.getLogger(AbstractQueryRunner.class.getName());

  private final PreparedStatements preparedStatements;
  private final PreparedStatementMapper preparedStatementMapper;
  private final ResultSetMapper resultSetMapper;

  private final Connection connection;

  /**
   * Constructs an {@code AbstractQueryRunner} with the required collaborators.
   *
   * @param preparedStatements the SQL query string source for this database engine
   * @param preparedStatementMapper the parameter binder for prepared statements
   * @param resultSetMapper the result set mapper for converting rows to domain objects
   * @param connection the JDBC connection; lifecycle owned by the caller
   */
  protected AbstractQueryRunner(
      final PreparedStatements preparedStatements,
      final PreparedStatementMapper preparedStatementMapper,
      final ResultSetMapper resultSetMapper,
      final Connection connection) {
    this.preparedStatements = preparedStatements;
    this.preparedStatementMapper = preparedStatementMapper;
    this.resultSetMapper = resultSetMapper;
    this.connection = connection;
  }

  @Override
  public List<Table> getTableInfo(final String schema) throws SQLException {
    try (final var preparedStatement = preparedStatements.tableInfoPreparedStatement(connection)) {
      preparedStatementMapper.prepareTableInfoStatement(preparedStatement, schema);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<Table> tables = resultSetMapper.mapToTables(resultSet);

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
  public List<Column> getColumnInfo(final String schema, final String tableName)
      throws SQLException {
    try (final var preparedStatement = preparedStatements.columnInfoPreparedStatement(connection)) {
      preparedStatementMapper.prepareColumnInfoStatement(preparedStatement, schema, tableName);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<Column> columns = resultSetMapper.mapToColumns(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} columns for table: {1} in schema: {2}",
              new Object[] {
                columns.size(), LogUtils.sanitizeForLog(tableName), LogUtils.sanitizeForLog(schema)
              });
        }
        return columns;
      }
    }
  }

  @Override
  public Optional<PrimaryKey> getPrimaryKeyInfo(final String schema, final String tableName)
      throws SQLException {
    try (final var preparedStatement =
        preparedStatements.primaryKeyInfoPreparedStatement(connection)) {
      preparedStatementMapper.preparePrimaryKeyInfoStatement(preparedStatement, schema, tableName);

      try (final var resultSet = preparedStatement.executeQuery()) {
        return resultSetMapper.mapToPrimaryKey(resultSet);
      }
    }
  }

  @Override
  public List<ForeignKey> getForeignKeyInfo(final String schema, final String tableName)
      throws SQLException {
    try (final var preparedStatement =
        preparedStatements.foreignKeyInfoPreparedStatement(connection)) {
      preparedStatementMapper.prepareForeignKeyInfoStatement(preparedStatement, schema, tableName);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<ForeignKey> foreignKeys = resultSetMapper.mapToForeignKeys(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} foreign keys for table: {1} in schema: {2}",
              new Object[] {
                foreignKeys.size(),
                LogUtils.sanitizeForLog(tableName),
                LogUtils.sanitizeForLog(schema)
              });
        }
        return foreignKeys;
      }
    }
  }
}
