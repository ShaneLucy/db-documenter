package db.documenter.internal.renderer.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
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
class RelationshipRendererTest {

  @Mock private MultiplicityFormatter multiplicityFormatter;

  private RelationshipRenderer relationshipRenderer;

  @BeforeEach
  void setUp() {
    reset(multiplicityFormatter);
    relationshipRenderer = new RelationshipRenderer(multiplicityFormatter);
  }

  @Nested
  class RenderTests {

    @Test
    void returnsEmptyStringWhenSchemaHasNoTables() {
      final var schema =
          Schema.builder()
              .name("public")
              .tables(List.of())
              .views(List.of())
              .materializedViews(List.of())
              .dbEnums(List.of())
              .compositeTypes(List.of())
              .build();

      final var result = relationshipRenderer.render(schema);

      assertEquals("", result);
      verifyNoInteractions(multiplicityFormatter);
    }

    @Test
    void returnsEmptyStringWhenSchemaTablesHaveNoForeignKeys() {
      final var table1 =
          Table.builder().name("users").foreignKeys(List.of()).columns(List.of()).build();
      final var table2 =
          Table.builder().name("orders").foreignKeys(List.of()).columns(List.of()).build();
      final var schema =
          Schema.builder()
              .name("public")
              .tables(List.of(table1, table2))
              .views(List.of())
              .materializedViews(List.of())
              .dbEnums(List.of())
              .compositeTypes(List.of())
              .build();

      final var result = relationshipRenderer.render(schema);

      assertEquals("", result);
      verifyNoInteractions(multiplicityFormatter);
    }

    @Test
    void rendersRelationshipsForSingleForeignKey() {
      final var foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var table =
          Table.builder()
              .name("orders")
              .foreignKeys(List.of(foreignKey))
              .columns(List.of())
              .build();
      final var schema =
          Schema.builder()
              .name("public")
              .tables(List.of(table))
              .views(List.of())
              .materializedViews(List.of())
              .dbEnums(List.of())
              .compositeTypes(List.of())
              .build();

      when(multiplicityFormatter.format(foreignKey, "public", null))
          .thenReturn("users ||--|{ orders");

      final var result = relationshipRenderer.render(schema);

      assertTrue(result.contains("users ||--|{ orders"));
      verify(multiplicityFormatter).format(foreignKey, "public", null);
    }

    @Test
    void rendersMultipleForeignKeysGroupedByTargetTable() {
      final var foreignKey1 =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var foreignKey2 =
          ForeignKey.builder()
              .name("fk_product")
              .sourceTable("orders")
              .sourceColumn("product_id")
              .targetTable("products")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(true)
              .build();

      final var table =
          Table.builder()
              .name("orders")
              .foreignKeys(List.of(foreignKey1, foreignKey2))
              .columns(List.of())
              .build();
      final var schema =
          Schema.builder()
              .name("public")
              .tables(List.of(table))
              .views(List.of())
              .materializedViews(List.of())
              .dbEnums(List.of())
              .compositeTypes(List.of())
              .build();

      when(multiplicityFormatter.format(foreignKey1, "public", null))
          .thenReturn("users ||--|{ orders");
      when(multiplicityFormatter.format(foreignKey2, "public", null))
          .thenReturn("products ||--o{ orders");

      final var result = relationshipRenderer.render(schema);

      assertTrue(result.contains("users ||--|{ orders"));
      assertTrue(result.contains("products ||--o{ orders"));
      verify(multiplicityFormatter).format(foreignKey1, "public", null);
      verify(multiplicityFormatter).format(foreignKey2, "public", null);
    }

    @Test
    void groupsRelationshipsByTargetTableAndSortsAlphabetically() {
      final var foreignKey1 =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var foreignKey2 =
          ForeignKey.builder()
              .name("fk_product")
              .sourceTable("orders")
              .sourceColumn("product_id")
              .targetTable("products")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var table =
          Table.builder()
              .name("orders")
              .foreignKeys(List.of(foreignKey1, foreignKey2))
              .columns(List.of())
              .build();
      final var schema =
          Schema.builder()
              .name("public")
              .tables(List.of(table))
              .views(List.of())
              .materializedViews(List.of())
              .dbEnums(List.of())
              .compositeTypes(List.of())
              .build();

      when(multiplicityFormatter.format(foreignKey1, "public", null))
          .thenReturn("users ||--|{ orders");
      when(multiplicityFormatter.format(foreignKey2, "public", null))
          .thenReturn("products ||--|{ orders");

      final var result = relationshipRenderer.render(schema);

      final int productsIndex = result.indexOf("products ||--|{ orders");
      final int usersIndex = result.indexOf("users ||--|{ orders");

      assertTrue(
          productsIndex < usersIndex,
          "Relationships should be sorted alphabetically by target table");
    }

    @Test
    void addsBlankLinesBetweenTargetTableGroups() {
      final var foreignKey1 =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var foreignKey2 =
          ForeignKey.builder()
              .name("fk_product")
              .sourceTable("order_items")
              .sourceColumn("product_id")
              .targetTable("products")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var table1 =
          Table.builder()
              .name("orders")
              .foreignKeys(List.of(foreignKey1))
              .columns(List.of())
              .build();
      final var table2 =
          Table.builder()
              .name("order_items")
              .foreignKeys(List.of(foreignKey2))
              .columns(List.of())
              .build();
      final var schema =
          Schema.builder()
              .name("public")
              .tables(List.of(table1, table2))
              .views(List.of())
              .materializedViews(List.of())
              .dbEnums(List.of())
              .compositeTypes(List.of())
              .build();

      when(multiplicityFormatter.format(foreignKey1, "public", null))
          .thenReturn("users ||--|{ orders");
      when(multiplicityFormatter.format(foreignKey2, "public", null))
          .thenReturn("products ||--|{ order_items");

      final var result = relationshipRenderer.render(schema);

      assertTrue(result.contains("\n\n"), "Should have blank lines between target table groups");
    }

    @Test
    void handlesMultipleForeignKeysToSameTargetTable() {
      final var foreignKey1 =
          ForeignKey.builder()
              .name("fk_created_by")
              .sourceTable("posts")
              .sourceColumn("created_by")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var foreignKey2 =
          ForeignKey.builder()
              .name("fk_updated_by")
              .sourceTable("posts")
              .sourceColumn("updated_by")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(true)
              .build();

      final var table =
          Table.builder()
              .name("posts")
              .foreignKeys(List.of(foreignKey1, foreignKey2))
              .columns(List.of())
              .build();
      final var schema =
          Schema.builder()
              .name("public")
              .tables(List.of(table))
              .views(List.of())
              .materializedViews(List.of())
              .dbEnums(List.of())
              .compositeTypes(List.of())
              .build();

      when(multiplicityFormatter.format(foreignKey1, "public", null))
          .thenReturn("users ||--|{ posts");
      when(multiplicityFormatter.format(foreignKey2, "public", null))
          .thenReturn("users ||--o{ posts");

      final var result = relationshipRenderer.render(schema);

      assertTrue(result.contains("users ||--|{ posts"));
      assertTrue(result.contains("users ||--o{ posts"));
      verify(multiplicityFormatter, times(2)).format(any(), eq("public"), eq(null));
    }
  }
}
