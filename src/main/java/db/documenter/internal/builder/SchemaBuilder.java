package db.documenter.internal.builder;

import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.QueryRunnerFactory;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Builds database schema information by orchestrating table, enum, and composite type builders. */
public final class SchemaBuilder {
  private static final Logger LOGGER = Logger.getLogger(SchemaBuilder.class.getName());

  private final ConnectionManager connectionManager;
  private final QueryRunnerFactory queryRunnerFactory;
  private final EnumBuilder enumBuilder;
  private final CompositeTypeBuilder compositeTypeBuilder;
  private final TableBuilder tableBuilder;

  public SchemaBuilder(
      final ConnectionManager connectionManager,
      final QueryRunnerFactory queryRunnerFactory,
      final EnumBuilder enumBuilder,
      final CompositeTypeBuilder compositeTypeBuilder,
      final TableBuilder tableBuilder) {
    this.connectionManager = connectionManager;
    this.queryRunnerFactory = queryRunnerFactory;
    this.enumBuilder = enumBuilder;
    this.compositeTypeBuilder = compositeTypeBuilder;
    this.tableBuilder = tableBuilder;
  }

  /**
   * Builds a list of schemas with their tables, enums, and composite types.
   *
   * @param schemaNames the list of schema names to build
   * @return list of {@link Schema} instances
   * @throws SQLException if database access fails
   */
  // Object array is only created when logging is enabled (guarded by isLoggable).
  // This is standard Java logging pattern for parameterized messages.
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  public List<Schema> buildSchemas(final List<String> schemaNames) throws SQLException {
    final List<Schema> result = new ArrayList<>();

    for (final String schemaName : schemaNames) {
      try (final var connection = connectionManager.getConnection()) {
        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(Level.INFO, "Building schema: {0}", LogUtils.sanitizeForLog(schemaName));
        }

        final var queryRunner = queryRunnerFactory.createQueryRunner(connection);

        final List<DbEnum> dbEnums = enumBuilder.buildEnums(queryRunner, schemaName);

        final Map<EnumKey, DbEnum> enumsByKey = enumBuilder.buildEnumKeys(dbEnums, schemaName);

        final List<DbCompositeType> compositeTypes =
            compositeTypeBuilder.buildCompositeTypes(queryRunner, schemaName);

        final Map<ColumnKey, UdtReference> columnUdtMappings =
            queryRunner.getColumnUdtMappings(schemaName);

        final List<Table> tables =
            tableBuilder.buildTables(queryRunner, schemaName, enumsByKey, columnUdtMappings);

        result.add(
            Schema.builder()
                .name(schemaName)
                .tables(tables)
                .dbEnums(dbEnums)
                .compositeTypes(compositeTypes)
                .build());

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Completed schema: {0} ({1} tables, {2} enums, {3} composite types)",
              new Object[] {
                LogUtils.sanitizeForLog(schemaName),
                tables.size(),
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
