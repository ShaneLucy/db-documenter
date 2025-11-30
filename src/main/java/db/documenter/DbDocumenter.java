package db.documenter;

import db.documenter.internal.builder.EnumBuilder;
import db.documenter.internal.builder.FormatterConfigurer;
import db.documenter.internal.builder.SchemaBuilder;
import db.documenter.internal.builder.TableBuilder;
import db.documenter.internal.connection.ConnectionManagerFactory;
import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.mapper.ColumnMapper;
import db.documenter.internal.mapper.ForeignKeyMapper;
import db.documenter.internal.mapper.TableMapper;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.queries.QueryRunnerFactory;
import db.documenter.internal.renderer.impl.EntityRenderer;
import db.documenter.internal.renderer.impl.EnumRenderer;
import db.documenter.internal.renderer.impl.RelationshipRenderer;
import db.documenter.internal.renderer.impl.SchemaRenderer;
import java.sql.SQLException;
import java.util.List;

/** Entrypoint to the DbDocumenter application. */
public final class DbDocumenter {
  private final DbDocumenterConfig dbDocumenterConfig;
  private final FormatterConfigurer formatterConfigurer;
  private final SchemaBuilder schemaBuilder;

  public DbDocumenter(final DbDocumenterConfig dbDocumenterConfig) {
    this.dbDocumenterConfig = dbDocumenterConfig;
    this.formatterConfigurer = new FormatterConfigurer();

    final ConnectionManager connectionManager =
        new ConnectionManagerFactory(dbDocumenterConfig).createConnectionManager();
    final QueryRunnerFactory queryRunnerFactory = new QueryRunnerFactory(dbDocumenterConfig);

    final ColumnMapper columnMapper = new ColumnMapper();
    final ForeignKeyMapper foreignKeyMapper = new ForeignKeyMapper();
    final TableMapper tableMapper = new TableMapper();

    final EnumBuilder enumBuilder = new EnumBuilder();
    final TableBuilder tableBuilder = new TableBuilder(columnMapper, foreignKeyMapper, tableMapper);

    this.schemaBuilder =
        new SchemaBuilder(connectionManager, queryRunnerFactory, enumBuilder, tableBuilder);
  }

  /**
   * Inspects the provided database schema(s) to generate a puml file.
   *
   * @return a formatted {@link String} representation of the database schema.
   * @throws SQLException if database access fails
   */
  public String generatePuml() throws SQLException {
    final var entityFormatter = formatterConfigurer.createEntityLineFormatter();
    final var multiplicityFormatter = formatterConfigurer.createMultiplicityFormatter();

    final var entityRenderer = new EntityRenderer(entityFormatter);
    final var relationShipRenderer = new RelationshipRenderer(multiplicityFormatter);
    final var enumRenderer = new EnumRenderer();
    final var schemaRenderer =
        new SchemaRenderer(entityRenderer, relationShipRenderer, enumRenderer);

    final List<Schema> schemas = schemaBuilder.buildSchemas(dbDocumenterConfig.schemas());

    return schemaRenderer.render(schemas);
  }
}
