package db.documenter;

import db.documenter.internal.builder.EnumBuilder;
import db.documenter.internal.builder.PostgresqlSchemaBuilder;
import db.documenter.internal.builder.SchemaBuilder;
import db.documenter.internal.builder.TableBuilder;
import db.documenter.internal.connection.ConnectionManagerFactory;
import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.formatter.FormatterConfigurer;
import db.documenter.internal.mapper.ColumnMapper;
import db.documenter.internal.mapper.ForeignKeyMapper;
import db.documenter.internal.mapper.TableMapper;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.renderer.impl.EntityRenderer;
import db.documenter.internal.renderer.impl.EnumRenderer;
import db.documenter.internal.renderer.impl.RelationshipRenderer;
import db.documenter.internal.renderer.impl.SchemaRenderer;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Entrypoint to the DbDocumenter application. */
public final class DbDocumenter {
  private static final Logger LOGGER = Logger.getLogger(DbDocumenter.class.getName());
  private final DbDocumenterConfig dbDocumenterConfig;
  private final FormatterConfigurer formatterConfigurer;
  private final SchemaBuilder schemaBuilder;

  /**
   * Creates a documenter for the given configuration.
   *
   * <p>No database connection is opened here; that happens in {@link #generate()}.
   *
   * @param dbDocumenterConfig what to connect to and which schemas to document
   */
  public DbDocumenter(final DbDocumenterConfig dbDocumenterConfig) {
    this.dbDocumenterConfig = dbDocumenterConfig;
    this.formatterConfigurer = new FormatterConfigurer();

    final ConnectionManager connectionManager =
        new ConnectionManagerFactory(dbDocumenterConfig).createConnectionManager();

    final ColumnMapper columnMapper = new ColumnMapper();
    final ForeignKeyMapper foreignKeyMapper = new ForeignKeyMapper();
    final TableMapper tableMapper = new TableMapper();

    final EnumBuilder enumBuilder = new EnumBuilder();
    final TableBuilder tableBuilder = new TableBuilder(columnMapper, foreignKeyMapper, tableMapper);

    this.schemaBuilder =
        new PostgresqlSchemaBuilder(
            dbDocumenterConfig, connectionManager, enumBuilder, tableBuilder);
  }

  /**
   * Inspects the provided database schema(s) and generates the PlantUML diagrams in a single pass
   * over the database.
   *
   * @return the ERD and, when any schema defines enums, the enum diagram
   * @throws SQLException if database access fails
   * @throws IllegalArgumentException if the metadata read from the database is invalid, which
   *     indicates an unexpected catalog shape rather than a caller error
   */
  public DiagramResult generate() throws SQLException {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(
          Level.INFO,
          "Starting PlantUML generation for schemas: {0}",
          LogUtils.sanitizeForLog(dbDocumenterConfig.schemas()));
    }

    final var entityFormatter = formatterConfigurer.createEntityLineFormatter();
    final var multiplicityFormatter = formatterConfigurer.createMultiplicityFormatter();

    final var entityRenderer = new EntityRenderer(entityFormatter);
    final var relationShipRenderer = new RelationshipRenderer(multiplicityFormatter);
    final var enumRenderer = new EnumRenderer();
    final var schemaRenderer =
        new SchemaRenderer(entityRenderer, relationShipRenderer, enumRenderer);

    final List<Schema> schemas = schemaBuilder.buildSchemas(dbDocumenterConfig.schemas());

    final var result = schemaRenderer.renderDiagrams(schemas);

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Successfully generated PlantUML output");
    }

    return result;
  }
}
