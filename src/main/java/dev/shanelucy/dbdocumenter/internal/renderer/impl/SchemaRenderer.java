package dev.shanelucy.dbdocumenter.internal.renderer.impl;

import dev.shanelucy.dbdocumenter.DiagramResult;
import dev.shanelucy.dbdocumenter.internal.models.db.Schema;
import dev.shanelucy.dbdocumenter.internal.utils.LogUtils;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders database schemas as a PlantUML entity-relationship diagram plus a separate enum diagram.
 *
 * <p>In the ERD each schema is wrapped in a {@code package} block containing its tables, followed
 * by all foreign key relationships. Enums are deliberately excluded from the ERD. The enum diagram
 * only contains {@code package} blocks for schemas that define enums, and does not hide stereotypes
 * so that {@code <<enum>>} stays visible.
 */
public final class SchemaRenderer {
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

  /**
   * Renders both diagrams for the given schemas.
   *
   * @param schemas schemas to render
   * @return the ERD, and the enum diagram when at least one schema defines an enum
   */
  public DiagramResult renderDiagrams(final List<Schema> schemas) {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Rendering PlantUML output");
    }

    return DiagramResult.builder()
        .erdDiagram(renderErd(schemas))
        .enumDiagram(renderEnums(schemas).orElse(null))
        .build();
  }

  private String renderErd(final List<Schema> schemas) {
    final var stringBuilder = new StringBuilder(256);
    stringBuilder.append("@startuml\nhide methods\nhide stereotypes\n\n");

    schemas.forEach(
        schema -> {
          stringBuilder.append(String.format("package \"%s\" {%n", schema.name()));

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

  private Optional<String> renderEnums(final List<Schema> schemas) {
    final var schemasWithEnums =
        schemas.stream().filter(schema -> !schema.dbEnums().isEmpty()).toList();
    if (schemasWithEnums.isEmpty()) {
      return Optional.empty();
    }

    final var stringBuilder = new StringBuilder(256);
    stringBuilder.append("@startuml\n\n");
    schemasWithEnums.forEach(
        schema -> {
          stringBuilder.append(String.format("package \"%s\" {%n", schema.name()));
          schema
              .dbEnums()
              .forEach(dbEnum -> stringBuilder.append(enumRenderer.render(dbEnum)).append('\n'));
          stringBuilder.append("}\n\n");
        });
    return Optional.of(stringBuilder.append("@enduml\n").toString());
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
