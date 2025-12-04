package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.Schema;
import db.documenter.internal.renderer.api.PumlRenderer;
import db.documenter.internal.utils.LogUtils;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SchemaRenderer implements PumlRenderer<List<Schema>> {
  private static final Logger LOGGER = Logger.getLogger(SchemaRenderer.class.getName());
  private final EntityRenderer entityRenderer;
  private final RelationshipRenderer relationshipRenderer;
  private final EnumRenderer enumRenderer;

  public SchemaRenderer(
      final EntityRenderer entityRenderer,
      final RelationshipRenderer relationshipRenderer,
      final EnumRenderer enumRenderer) {
    this.entityRenderer = entityRenderer;
    this.relationshipRenderer = relationshipRenderer;
    this.enumRenderer = enumRenderer;
  }

  @Override
  public String render(final List<Schema> schemas) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Rendering PlantUML output");
    }

    final var stringBuilder = new StringBuilder();
    stringBuilder.append("@startuml\n");
    stringBuilder.append("hide methods\nhide stereotypes\n\n");

    schemas.forEach(
        schema -> {
          stringBuilder.append(String.format("package \"%s\" {%n", schema.name()));

          schema
              .dbEnums()
              .forEach(dbEnum -> stringBuilder.append(enumRenderer.render(dbEnum)).append("\n"));

          schema
              .tables()
              .forEach(table -> stringBuilder.append(entityRenderer.render(table)).append("\n"));

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
          "Rendered {0} schema with {1} table(s), {2} enum(s), {3} relationship(s)",
          new Object[] {
            LogUtils.sanitizeForLog(schema.name()),
            schema.tables().size(),
            schema.dbEnums().size(),
            totalRelationships
          });
    }
  }
}
