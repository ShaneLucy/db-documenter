package db.documenter.internal.formatter.impl.multiplicity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import db.documenter.internal.models.db.ForeignKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultMultiplicityFormatterTest {

  private DefaultMultiplicityFormatter defaultMultiplicityFormatter;

  @BeforeEach
  void setUp() {
    defaultMultiplicityFormatter = new DefaultMultiplicityFormatter();
  }

  @Nested
  class FormatTests {

    @Test
    void whenCurrentIsNullReturnsFormattedRelationship() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .name("fk_orders_users")
              .build();

      final var result = defaultMultiplicityFormatter.format(fk, "public", null);

      assertEquals("users -- orders", result);
    }

    @Test
    void whenCurrentIsNotNullReturnsCurrent() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("orders")
              .targetTable("users")
              .referencedSchema("public")
              .name("fk_test")
              .build();

      final var result = defaultMultiplicityFormatter.format(fk, "public", "existing value");

      assertEquals("existing value", result);
    }

    @Test
    void formatsWithDifferentTableNames() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("order_items")
              .sourceColumn("product_id")
              .targetTable("products")
              .targetColumn("id")
              .referencedSchema("public")
              .name("fk_items_products")
              .build();

      final var result = defaultMultiplicityFormatter.format(fk, "public", null);

      assertEquals("products -- order_items", result);
    }

    @Test
    void handlesEmptyStrings() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("")
              .sourceColumn("")
              .targetTable("")
              .targetColumn("")
              .referencedSchema("")
              .name("")
              .build();

      final var result = defaultMultiplicityFormatter.format(fk, "", null);

      assertEquals(" -- ", result);
    }

    @Test
    void handlesWhitespaceInTableNames() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("source table")
              .sourceColumn("col")
              .targetTable("target table")
              .targetColumn("id")
              .referencedSchema("public")
              .name("fk")
              .build();

      final var result = defaultMultiplicityFormatter.format(fk, "public", null);

      assertEquals("target table -- source table", result);
    }

    @Test
    void formatsCrossSchemaForeignKeyWithBothTablesQualified() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("other_schema")
              .name("fk_orders_users")
              .build();

      final var result = defaultMultiplicityFormatter.format(fk, "public", null);

      assertEquals("other_schema.users -- public.orders", result);
    }

    @Test
    void formatsNullReferencedSchemaAsCrossSchema() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema(null)
              .name("fk_orders_users")
              .build();

      final var result = defaultMultiplicityFormatter.format(fk, "public", null);

      assertEquals("users -- orders", result);
    }
  }
}
