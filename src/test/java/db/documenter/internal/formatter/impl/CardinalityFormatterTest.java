package db.documenter.internal.formatter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import db.documenter.internal.models.db.ForeignKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CardinalityFormatterTest {

  private CardinalityFormatter cardinalityFormatter;

  @BeforeEach
  void setUp() {
    cardinalityFormatter = new CardinalityFormatter();
  }

  @Nested
  class FormatTests {

    @Test
    void whenForeignKeyIsNullableUsesZeroOrManyCardinality() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("orders")
              .targetTable("users")
              .name("fk_test")
              .isNullable(true)
              .build();

      final var result = cardinalityFormatter.format(fk, "users -- orders");

      assertEquals("users ||--o{ orders", result);
    }

    @Test
    void whenForeignKeyIsNotNullableUsesOneOrManyCardinality() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("orders")
              .targetTable("users")
              .name("fk_test")
              .isNullable(false)
              .build();

      final var result = cardinalityFormatter.format(fk, "users -- orders");

      assertEquals("users ||--|{ orders", result);
    }

    @Test
    void replacesBasicConnectorWithCrowsFoot() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("order_items")
              .targetTable("products")
              .name("fk_items_products")
              .isNullable(true)
              .build();

      final var result = cardinalityFormatter.format(fk, "products -- order_items");

      assertEquals("products ||--o{ order_items", result);
    }

    @Test
    void handlesMultipleSpacesAroundConnector() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("source")
              .targetTable("target")
              .name("fk")
              .isNullable(false)
              .build();

      final var result = cardinalityFormatter.format(fk, "target  --  source");

      assertEquals("target  ||--|{  source", result);
    }

    @Test
    void doesNotReplaceIfNoBasicConnector() {
      final var fk =
          ForeignKey.builder()
              .sourceTable("orders")
              .targetTable("users")
              .name("fk")
              .isNullable(true)
              .build();

      final var result = cardinalityFormatter.format(fk, "users||orders");

      assertEquals("users||orders", result);
    }
  }
}
