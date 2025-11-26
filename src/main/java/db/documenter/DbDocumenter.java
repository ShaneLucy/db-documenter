package db.documenter;

import db.documenter.internal.db.api.ConnectionManager;
import db.documenter.internal.db.impl.PostgresConnectionManager;
import db.documenter.internal.formatter.impl.CompositeLineFormatter;
import db.documenter.internal.formatter.impl.DefaultLineFormatter;
import db.documenter.internal.formatter.impl.ForeignKeyLineFormatter;
import db.documenter.internal.formatter.impl.NullableLineFormatter;
import db.documenter.internal.formatter.impl.PrimaryKeyLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.QueryRunnerFactory;
import db.documenter.internal.renderer.impl.EntityRenderer;
import db.documenter.internal.renderer.impl.RelationshipRenderer;
import db.documenter.internal.renderer.impl.SchemaRenderer;
import java.sql.SQLException;
import java.util.List;

/** Entrypoint to the DbDocumenter application. */
public final class DbDocumenter {
  private final ConnectionManager connectionManager;
  private final DbDocumenterConfig dbDocumenterConfig;
  private final QueryRunnerFactory queryRunnerFactory;

  public DbDocumenter(final DbDocumenterConfig dbDocumenterConfig) {
    this.connectionManager = new PostgresConnectionManager(dbDocumenterConfig);
    this.queryRunnerFactory = new QueryRunnerFactory(dbDocumenterConfig);
    this.dbDocumenterConfig = dbDocumenterConfig;
  }

  /**
   * Inspects the provided database schema(s) to generate a puml file.
   *
   * @return a formatted {@link String} representation of the database schema.
   */
  public String generatePuml() {
    final var formatter =
        CompositeLineFormatter.builder()
            .addFormatter(new DefaultLineFormatter())
            .addFormatter(new PrimaryKeyLineFormatter())
            .addFormatter(new ForeignKeyLineFormatter())
            .addFormatter(new NullableLineFormatter())
            .build();
    final var entityRenderer = new EntityRenderer(formatter);
    final var relationShipRenderer = new RelationshipRenderer();
    final var schemaRenderer = new SchemaRenderer(entityRenderer, relationShipRenderer);
    final List<Schema> schemas = buildSchemas();

    return schemaRenderer.render(schemas);
  }

  private List<Schema> buildSchemas() {
    return dbDocumenterConfig.schemas().stream()
        .map(
            schema -> {
              try {
                return Schema.builder().name(schema).tables(buildTables(schema)).build();
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  private List<Table> buildTables(final String schema) throws SQLException {
    try (final var connection = connectionManager.getConnection()) {
      final var queryRunner = queryRunnerFactory.createQueryRunner(connection);
      final var tables = queryRunner.getTableInfo(schema);

      return tables.stream()
          .map(
              table -> {
                try {
                  final List<Column> columns = queryRunner.getColumnInfo(schema, table);

                  final var primaryKey = queryRunner.getPrimaryKeyInfo(schema, table);

                  final List<ForeignKey> foreignKeys = queryRunner.getForeignKeyInfo(schema, table);

                  return Table.combineTableColumnsPrimaryAndForeignKeys(
                      table, columns, primaryKey, foreignKeys);
                } catch (SQLException e) {
                  throw new RuntimeException(e);
                }
              })
          .toList();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
