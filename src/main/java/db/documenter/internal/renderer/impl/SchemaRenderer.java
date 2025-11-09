package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.Schema;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;

public class SchemaRenderer implements PumlRenderer<List<Schema>> {
  private final EntityRenderer entityRenderer;
  private final RelationshipRenderer relationshipRenderer;

  public SchemaRenderer(EntityRenderer entityRenderer, RelationshipRenderer relationshipRenderer) {
    this.entityRenderer = entityRenderer;
    this.relationshipRenderer = relationshipRenderer;
  }

  @Override
  public String render(final List<Schema> schemas) {
    StringBuilder sb = new StringBuilder();
    sb.append("@startuml\n");
    sb.append("hide methods\nhide stereotypes\n\n");

    schemas.forEach(
        schema -> {
          sb.append(String.format("package \"%s\" {%n", schema.name()));
          // Entities
          schema.tables().forEach(table -> sb.append(entityRenderer.render(table)).append("\n"));

          sb.append("}\n");
        });

    // Relationships
    schemas.forEach(schema -> sb.append(relationshipRenderer.render(schema.tables())));

    sb.append("@enduml\n");
    return sb.toString();
  }
}
