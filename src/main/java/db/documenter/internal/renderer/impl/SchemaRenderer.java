package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.Schema;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;

public final class SchemaRenderer implements PumlRenderer<List<Schema>> {
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

          stringBuilder.append("}\n");
        });

    stringBuilder.append("\n");

    schemas.forEach(schema -> stringBuilder.append(relationshipRenderer.render(schema.tables())));

    stringBuilder.append("@enduml\n");
    return stringBuilder.toString();
  }
}
