package db.documenter.internal.renderer.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchemaRendererTest {

  @Mock private EntityRenderer entityRenderer;
  @Mock private RelationshipRenderer relationshipRenderer;
  @Mock private EnumRenderer enumRenderer;

  private SchemaRenderer schemaRenderer;

  @BeforeEach
  void setUp() {
    reset(entityRenderer, relationshipRenderer, enumRenderer);
    schemaRenderer = new SchemaRenderer(entityRenderer, relationshipRenderer, enumRenderer);
  }

  @Nested
  class RenderTests {

    @Test
    void rendersEmptyPlantUmlWhenNoSchemas() {
      final var result = schemaRenderer.render(List.of());

      assertTrue(result.contains("@startuml"));
      assertTrue(result.contains("@enduml"));
      assertTrue(result.contains("hide methods"));
      assertTrue(result.contains("hide stereotypes"));
      verifyNoInteractions(entityRenderer, relationshipRenderer, enumRenderer);
    }

    @Test
    void rendersSchemaPackageWithName() {
      final var schema = Schema.builder().name("public").tables(List.of()).build();

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("package \"public\" {"));
      assertTrue(result.contains("}"));
      verifyNoInteractions(entityRenderer, enumRenderer);
    }

    @Test
    void rendersTableUsingEntityRenderer() {
      final var table = Table.builder().name("users").build();
      final var schema = Schema.builder().name("public").tables(List.of(table)).build();

      when(entityRenderer.render(table)).thenReturn("entity users {");

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("entity users {"));
      verify(entityRenderer).render(table);
    }

    @Test
    void rendersMultipleTables() {
      final var table1 = Table.builder().name("users").build();
      final var table2 = Table.builder().name("orders").build();
      final var schema = Schema.builder().name("public").tables(List.of(table1, table2)).build();

      when(entityRenderer.render(table1)).thenReturn("entity users {");
      when(entityRenderer.render(table2)).thenReturn("entity orders {");

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("entity users {"));
      assertTrue(result.contains("entity orders {"));
      verify(entityRenderer).render(table1);
      verify(entityRenderer).render(table2);
    }

    @Test
    void rendersEnumUsingEnumRenderer() {
      final var dbEnum =
          DbEnum.builder().enumName("status").enumValues(List.of("ACTIVE", "INACTIVE")).build();
      final var schema = Schema.builder().name("public").dbEnums(List.of(dbEnum)).build();

      when(enumRenderer.render(dbEnum)).thenReturn("enum status {");

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("enum status {"));
      verify(enumRenderer).render(dbEnum);
    }

    @Test
    void rendersMultipleEnums() {
      final var dbEnum1 =
          DbEnum.builder().enumName("status").enumValues(List.of("ACTIVE", "INACTIVE")).build();
      final var dbEnum2 =
          DbEnum.builder().enumName("role").enumValues(List.of("ADMIN", "USER")).build();
      final var schema = Schema.builder().name("public").dbEnums(List.of(dbEnum1, dbEnum2)).build();

      when(enumRenderer.render(dbEnum1)).thenReturn("enum status {");
      when(enumRenderer.render(dbEnum2)).thenReturn("enum role {");

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("enum status {"));
      assertTrue(result.contains("enum role {"));
      verify(enumRenderer).render(dbEnum1);
      verify(enumRenderer).render(dbEnum2);
    }

    @Test
    void rendersRelationshipsUsingRelationshipRenderer() {
      final var foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final var table = Table.builder().name("orders").foreignKeys(List.of(foreignKey)).build();
      final var schema = Schema.builder().name("public").tables(List.of(table)).build();

      when(entityRenderer.render(table)).thenReturn("entity orders {");
      when(relationshipRenderer.render(schema)).thenReturn("users ||--|{ orders\n");

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("users ||--|{ orders"));
      verify(relationshipRenderer).render(schema);
    }

    @Test
    void rendersMultipleSchemas() {
      final var table1 = Table.builder().name("users").build();
      final var schema1 = Schema.builder().name("public").tables(List.of(table1)).build();

      final var table2 = Table.builder().name("products").build();
      final var schema2 = Schema.builder().name("inventory").tables(List.of(table2)).build();

      when(entityRenderer.render(table1)).thenReturn("entity users {");
      when(entityRenderer.render(table2)).thenReturn("entity products {");
      when(relationshipRenderer.render(schema1)).thenReturn("");
      when(relationshipRenderer.render(schema2)).thenReturn("");

      final var result = schemaRenderer.render(List.of(schema1, schema2));

      assertTrue(result.contains("package \"public\" {"));
      assertTrue(result.contains("package \"inventory\" {"));
      assertTrue(result.contains("entity users {"));
      assertTrue(result.contains("entity products {"));
      verify(relationshipRenderer).render(schema1);
      verify(relationshipRenderer).render(schema2);
    }

    @Test
    void rendersEnumsBeforeTables() {
      final var dbEnum = DbEnum.builder().enumName("status").enumValues(List.of("ACTIVE")).build();
      final var table = Table.builder().name("users").build();
      final var schema =
          Schema.builder().name("public").dbEnums(List.of(dbEnum)).tables(List.of(table)).build();

      when(enumRenderer.render(dbEnum)).thenReturn("enum status {\nACTIVE\n}");
      when(entityRenderer.render(table)).thenReturn("entity users {");
      when(relationshipRenderer.render(schema)).thenReturn("");

      final var result = schemaRenderer.render(List.of(schema));

      final int enumIndex = result.indexOf("enum status {");
      final int tableIndex = result.indexOf("entity users {");

      assertTrue(enumIndex < tableIndex, "Enums should be rendered before tables");
    }

    @Test
    void rendersTablesBeforeRelationships() {
      final var foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final var table = Table.builder().name("orders").foreignKeys(List.of(foreignKey)).build();
      final var schema = Schema.builder().name("public").tables(List.of(table)).build();

      when(entityRenderer.render(table)).thenReturn("entity orders {");
      when(relationshipRenderer.render(schema)).thenReturn("users ||--|{ orders");

      final var result = schemaRenderer.render(List.of(schema));

      final int tableIndex = result.indexOf("entity orders {");
      final int relationshipIndex = result.indexOf("users ||--|{ orders");

      assertTrue(tableIndex < relationshipIndex, "Tables should be rendered before relationships");
    }

    @Test
    void includesPlantUmlStartAndEndMarkers() {
      final var schema = Schema.builder().name("public").build();

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.startsWith("@startuml"));
      assertTrue(result.trim().endsWith("@enduml"));
    }

    @Test
    void includesHideMethodsAndStereotypesDirectives() {
      final var schema = Schema.builder().name("public").build();

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("hide methods"));
      assertTrue(result.contains("hide stereotypes"));
    }

    @Test
    void rendersCompleteSchemaWithEnumsTablesAndRelationships() {
      final var dbEnum =
          DbEnum.builder().enumName("status").enumValues(List.of("ACTIVE", "INACTIVE")).build();

      final var table1 = Table.builder().name("users").build();

      final var foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final var table2 = Table.builder().name("orders").foreignKeys(List.of(foreignKey)).build();

      final var schema =
          Schema.builder()
              .name("public")
              .dbEnums(List.of(dbEnum))
              .tables(List.of(table1, table2))
              .build();

      when(enumRenderer.render(dbEnum)).thenReturn("enum status {");
      when(entityRenderer.render(table1)).thenReturn("entity users {");
      when(entityRenderer.render(table2)).thenReturn("entity orders {");
      when(relationshipRenderer.render(schema)).thenReturn("users ||--|{ orders");

      final var result = schemaRenderer.render(List.of(schema));

      assertTrue(result.contains("@startuml"));
      assertTrue(result.contains("hide methods"));
      assertTrue(result.contains("hide stereotypes"));
      assertTrue(result.contains("package \"public\" {"));
      assertTrue(result.contains("enum status {"));
      assertTrue(result.contains("entity users {"));
      assertTrue(result.contains("entity orders {"));
      assertTrue(result.contains("users ||--|{ orders"));
      assertTrue(result.contains("@enduml"));

      verify(enumRenderer).render(dbEnum);
      verify(entityRenderer).render(table1);
      verify(entityRenderer).render(table2);
      verify(relationshipRenderer).render(schema);
    }
  }
}
