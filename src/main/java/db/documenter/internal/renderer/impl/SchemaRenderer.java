package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.Schema;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;

public final class SchemaRenderer implements PumlRenderer<List<Schema>> {
  private final EntityRenderer entityRenderer;
  private final RelationshipRenderer relationshipRenderer;

  public SchemaRenderer(
      final EntityRenderer entityRenderer, final RelationshipRenderer relationshipRenderer) {
    this.entityRenderer = entityRenderer;
    this.relationshipRenderer = relationshipRenderer;
  }

  @Override
  public String render(final List<Schema> schemas) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append("@startuml\n");
    stringBuilder.append("hide methods\nhide stereotypes\n\n");

    schemas.forEach(
        schema -> {
          stringBuilder.append(String.format("package \"%s\" {%n", schema.name()));
          // Entities
          schema
              .tables()
              .forEach(table -> stringBuilder.append(entityRenderer.render(table)).append("\n"));

          stringBuilder.append("}\n");
        });

    stringBuilder.append("\n");
    // Relationships
    schemas.forEach(schema -> stringBuilder.append(relationshipRenderer.render(schema.tables())));

    stringBuilder.append("@enduml\n");
    return stringBuilder.toString();
  }
}
