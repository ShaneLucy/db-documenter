package db.documenter.internal.builder;

import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.QueryRunnerFactory;
import java.sql.SQLException;
import java.util.List;

/** Builds database schema information by orchestrating table and enum builders. */
public final class SchemaBuilder {
  private final ConnectionManager connectionManager;
  private final QueryRunnerFactory queryRunnerFactory;
  private final EnumBuilder enumBuilder;
  private final TableBuilder tableBuilder;

  public SchemaBuilder(
      final ConnectionManager connectionManager,
      final QueryRunnerFactory queryRunnerFactory,
      final EnumBuilder enumBuilder,
      final TableBuilder tableBuilder) {
    this.connectionManager = connectionManager;
    this.queryRunnerFactory = queryRunnerFactory;
    this.enumBuilder = enumBuilder;
    this.tableBuilder = tableBuilder;
  }

  /**
   * Builds a list of schemas with their tables and enums.
   *
   * @param schemaNames the list of schema names to build
   * @return list of {@link Schema} instances
   */
  public List<Schema> buildSchemas(final List<String> schemaNames) {
    return schemaNames.stream()
        .map(
            schemaName -> {
              try (final var connection = connectionManager.getConnection()) {
                final var queryRunner = queryRunnerFactory.createQueryRunner(connection);

                final List<DbEnum> dbEnums = enumBuilder.buildEnums(queryRunner, schemaName);
                final List<Table> tables =
                    tableBuilder.buildTables(queryRunner, schemaName, dbEnums);

                return Schema.builder().name(schemaName).tables(tables).dbEnums(dbEnums).build();
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }
}
