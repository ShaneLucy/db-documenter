package db.documenter.internal.builder;

import db.documenter.DbDocumenterConfig;
import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.View;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.impl.postgresql.PostgresqlPreparedStatements;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper;
import db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PostgreSQL-specific implementation of {@link SchemaBuilder}.
 *
 * <p>Orchestrates table, view, enum, composite type, and materialized view builders to produce a
 * complete list of {@link Schema} instances. Constructs a {@link PostgresqlQueryRunner} directly
 * from a JDBC connection for each schema, using {@link PostgresqlPreparedStatementMapper} and
 * {@link PostgresqlResultSetMapper} as collaborators.
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
  private final CompositeTypeBuilder compositeTypeBuilder;
  private final TableBuilder tableBuilder;
  private final ViewBuilder viewBuilder;

  /**
   * Creates a new {@code PostgresqlSchemaBuilder} with the required collaborators.
   *
   * @param config the application configuration (reserved for future use, e.g., query options)
   * @param connectionManager the source of JDBC connections; one connection is opened per schema
   * @param enumBuilder builds enum type information for a schema
   * @param compositeTypeBuilder builds composite type information for a schema
   * @param tableBuilder builds table information for a schema
   * @param viewBuilder builds view and materialized view information for a schema
   */
  public PostgresqlSchemaBuilder(
      final DbDocumenterConfig config,
      final ConnectionManager connectionManager,
      final EnumBuilder enumBuilder,
      final CompositeTypeBuilder compositeTypeBuilder,
      final TableBuilder tableBuilder,
      final ViewBuilder viewBuilder) {
    this.connectionManager = connectionManager;
    this.enumBuilder = enumBuilder;
    this.compositeTypeBuilder = compositeTypeBuilder;
    this.tableBuilder = tableBuilder;
    this.viewBuilder = viewBuilder;
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

        final List<DbCompositeType> compositeTypes =
            compositeTypeBuilder.buildCompositeTypes(queryRunner, schemaName);

        final Map<ColumnKey, UdtReference> columnUdtMappings =
            queryRunner.getColumnUdtMappings(schemaName);

        final List<Table> tables =
            tableBuilder.buildTables(queryRunner, schemaName, enumsByKey, columnUdtMappings);

        final List<View> views =
            viewBuilder.buildViews(queryRunner, schemaName, enumsByKey, columnUdtMappings);

        final List<MaterializedView> materializedViews =
            viewBuilder.buildMaterializedViews(
                queryRunner, schemaName, enumsByKey, columnUdtMappings);

        result.add(
            Schema.builder()
                .name(schemaName)
                .tables(tables)
                .views(views)
                .materializedViews(materializedViews)
                .dbEnums(dbEnums)
                .compositeTypes(compositeTypes)
                .build());

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Completed schema: {0} ({1} tables, {2} views, {3} materialized views, {4} enums, {5} composite types)",
              new Object[] {
                LogUtils.sanitizeForLog(schemaName),
                tables.size(),
                views.size(),
                materializedViews.size(),
                dbEnums.size(),
                compositeTypes.size()
              });
        }
      } catch (final SQLException e) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
          LOGGER.log(
              Level.SEVERE,
              "Failed to build schema: {0} - {1}",
              new Object[] {
                LogUtils.sanitizeForLog(schemaName), LogUtils.sanitizeForLog(e.getMessage())
              });
        }
        throw e;
      }
    }

    return result;
  }
}
