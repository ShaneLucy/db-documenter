package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.Schema;
import db.documenter.internal.renderer.api.PumlRenderer;
import db.documenter.internal.utils.LogUtils;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders a list of database schemas as a complete PlantUML entity-relationship diagram.
 *
 * <p>Each schema is wrapped in a PlantUML {@code package} block. Within each package, objects are
 * rendered in this order: enums, composite types, views, materialized views, tables. Foreign key
 * relationships are rendered after all packages.
 */
public final class SchemaRenderer implements PumlRenderer<List<Schema>> {
  private static final Logger LOGGER = Logger.getLogger(SchemaRenderer.class.getName());
  private final EntityRenderer entityRenderer;
  private final RelationshipRenderer relationshipRenderer;
  private final EnumRenderer enumRenderer;
  private final CompositeTypeRenderer compositeTypeRenderer;
  private final ViewRenderer viewRenderer;
  private final MaterializedViewRenderer materializedViewRenderer;

  public SchemaRenderer(
      final EntityRenderer entityRenderer,
      final RelationshipRenderer relationshipRenderer,
      final EnumRenderer enumRenderer,
      final CompositeTypeRenderer compositeTypeRenderer,
      final ViewRenderer viewRenderer,
      final MaterializedViewRenderer materializedViewRenderer) {
    this.entityRenderer = entityRenderer;
    this.relationshipRenderer = relationshipRenderer;
    this.enumRenderer = enumRenderer;
    this.compositeTypeRenderer = compositeTypeRenderer;
    this.viewRenderer = viewRenderer;
    this.materializedViewRenderer = materializedViewRenderer;
  }

  @Override
  public String render(final List<Schema> schemas) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Rendering PlantUML output");
    }

    final var stringBuilder = new StringBuilder(256);
    stringBuilder.append("@startuml\nhide methods\nhide stereotypes\n\n");

    schemas.forEach(
        schema -> {
          stringBuilder.append(String.format("package \"%s\" {%n", schema.name()));

          schema
              .dbEnums()
              .forEach(dbEnum -> stringBuilder.append(enumRenderer.render(dbEnum)).append('\n'));

          schema
              .compositeTypes()
              .forEach(
                  compositeType ->
                      stringBuilder
                          .append(compositeTypeRenderer.render(compositeType))
                          .append('\n'));

          schema
              .views()
              .forEach(view -> stringBuilder.append(viewRenderer.render(view)).append('\n'));

          schema
              .materializedViews()
              .forEach(
                  matView ->
                      stringBuilder.append(materializedViewRenderer.render(matView)).append('\n'));

          schema
              .tables()
              .forEach(table -> stringBuilder.append(entityRenderer.render(table)).append('\n'));

          logSchemaInfo(schema);
          stringBuilder.append("}\n\n");
        });

    schemas.forEach(schema -> stringBuilder.append(relationshipRenderer.render(schema)));

    stringBuilder.append("@enduml\n");

    return stringBuilder.toString();
  }

  private void logSchemaInfo(final Schema schema) {
    if (LOGGER.isLoggable(Level.INFO)) {
      final int totalRelationships =
          schema.tables().stream().mapToInt(table -> table.foreignKeys().size()).sum();

      LOGGER.log(
          Level.INFO,
          "Rendered {0} schema with {1} table(s), {2} view(s), {3} materialized view(s), {4} enum(s), {5} composite type(s), {6} relationship(s)",
          new Object[] {
            LogUtils.sanitizeForLog(schema.name()),
            schema.tables().size(),
            schema.views().size(),
            schema.materializedViews().size(),
            schema.dbEnums().size(),
            schema.compositeTypes().size(),
            totalRelationships
          });
    }
  }
}
