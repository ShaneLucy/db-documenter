package dev.shanelucy.dbdocumenter.internal.builder;

import dev.shanelucy.dbdocumenter.DbDocumenterConfig;
import dev.shanelucy.dbdocumenter.internal.connection.api.ConnectionManager;
import dev.shanelucy.dbdocumenter.internal.models.db.ColumnKey;
import dev.shanelucy.dbdocumenter.internal.models.db.DbEnum;
import dev.shanelucy.dbdocumenter.internal.models.db.Schema;
import dev.shanelucy.dbdocumenter.internal.models.db.Table;
import dev.shanelucy.dbdocumenter.internal.models.db.postgresql.EnumKey;
import dev.shanelucy.dbdocumenter.internal.models.db.postgresql.UdtReference;
import dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.PostgresqlPreparedStatements;
import dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper;
import dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import dev.shanelucy.dbdocumenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PostgreSQL-specific implementation of {@link SchemaBuilder}.
 *
 * <p>Orchestrates table and enum builders to produce a complete list of {@link Schema} instances.
 * Constructs a {@link PostgresqlQueryRunner} directly from a JDBC connection for each schema, using
 * {@link PostgresqlPreparedStatementMapper} and {@link PostgresqlResultSetMapper} as collaborators.
 *
 * <p><b>Resource Management:</b> Each schema build opens a new {@link java.sql.Connection} via the
 * {@link ConnectionManager} and closes it in a try-with-resources block, guaranteeing closure even
 * when an exception occurs.
 *
 * @see SchemaBuilder
 * @see PostgresqlQueryRunner
 */
public final class PostgresqlSchemaBuilder implements SchemaBuilder {

  private static final Logger LOGGER = Logger.getLogger(PostgresqlSchemaBuilder.class.getName());

  private final ConnectionManager connectionManager;
  private final EnumBuilder enumBuilder;
  private final TableBuilder tableBuilder;

  /**
   * Creates a new {@code PostgresqlSchemaBuilder} with the required collaborators.
   *
   * @param config the application configuration (reserved for future use, e.g., query options)
   * @param connectionManager the source of JDBC connections; one connection is opened per schema
   * @param enumBuilder builds enum type information for a schema
   * @param tableBuilder builds table information for a schema
   */
  public PostgresqlSchemaBuilder(
      final DbDocumenterConfig config,
      final ConnectionManager connectionManager,
      final EnumBuilder enumBuilder,
      final TableBuilder tableBuilder) {
    this.connectionManager = connectionManager;
    this.enumBuilder = enumBuilder;
    this.tableBuilder = tableBuilder;
  }

  /**
   * {@inheritDoc}
   *
   * <p>For each schema name, opens a new connection, constructs a {@link PostgresqlQueryRunner},
   * and delegates to the configured builders. The connection is closed after each schema regardless
   * of success or failure.
   */
  // Object array is only created when logging is enabled (guarded by isLoggable).
  // This is standard Java logging pattern for parameterized messages.
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Override
  public List<Schema> buildSchemas(final List<String> schemaNames) throws SQLException {
    final List<Schema> result = new ArrayList<>();

    for (final String schemaName : schemaNames) {
      try (final var connection = connectionManager.getConnection()) {
        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(Level.INFO, "Building schema: {0}", LogUtils.sanitizeForLog(schemaName));
        }

        final var queryRunner =
            new PostgresqlQueryRunner(
                new PostgresqlPreparedStatements(),
                new PostgresqlPreparedStatementMapper(),
                new PostgresqlResultSetMapper(),
                connection);

        final List<DbEnum> dbEnums = enumBuilder.buildEnums(queryRunner, schemaName);

        final Map<EnumKey, DbEnum> enumsByKey = enumBuilder.buildEnumKeys(dbEnums, schemaName);

        final Map<ColumnKey, UdtReference> columnUdtMappings =
            queryRunner.getColumnUdtMappings(schemaName);

        final List<Table> tables =
            tableBuilder.buildTables(queryRunner, schemaName, enumsByKey, columnUdtMappings);

        result.add(Schema.builder().name(schemaName).tables(tables).dbEnums(dbEnums).build());

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Completed schema: {0} ({1} tables, {2} enums)",
              new Object[] {LogUtils.sanitizeForLog(schemaName), tables.size(), dbEnums.size()});
        }
      } catch (final SQLException e) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
          LOGGER.log(
              Level.SEVERE,
              "Failed to build schema: {0} - {1}",
              new Object[] {
                LogUtils.sanitizeForLog(schemaName),
                LogUtils.sanitizeForLog(
                    Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()))
              });
        }
        throw e;
      }
    }

    return result;
  }
}
