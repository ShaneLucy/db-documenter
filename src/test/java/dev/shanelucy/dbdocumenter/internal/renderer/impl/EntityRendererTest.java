package db.documenter.internal.renderer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityRendererTest {

  @Mock private EntityLineFormatter lineFormatter;

  private EntityRenderer entityRenderer;

  @BeforeEach
  void setUp() {
    entityRenderer = new EntityRenderer(lineFormatter);
  }

  @Nested
  class RenderTableTests {

    @Test
    void itRendersTableWithNoColumnsAndNoPrimaryKey() {
      final var table =
          Table.builder().name("orders").columns(List.of()).foreignKeys(List.of()).build();

      final var result = entityRenderer.render(table);

      final var expected =
          """
          \tentity "orders" {
          \t}
          """;

      assertEquals(expected, result);
    }

    @Test
    void itRendersPrimaryKeyColumnAboveSeparatorAndNonPkColumnBelow() {
      final var idColumn =
          Column.builder()
              .isNullable(false)
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var nameColumn =
          Column.builder()
              .isNullable(false)
              .name("name")
              .dataType("varchar")
              .maximumLength(100)
              .constraints(List.of())
              .build();
      final var primaryKey =
          PrimaryKey.builder().constraintName("pk_orders").columnNames(List.of("id")).build();
      final var table =
          Table.builder()
              .name("orders")
              .columns(List.of(idColumn, nameColumn))
              .primaryKey(primaryKey)
              .foreignKeys(List.of())
              .build();

      when(lineFormatter.format(any(Table.class), any(Column.class), isNull()))
          .thenReturn("id: uuid")
          .thenReturn("name: varchar(100)");

      final var result = entityRenderer.render(table);

      final int idIndex = result.indexOf("\t\tid: uuid");
      final int separatorIndex = result.indexOf("\t\t--\n");
      final int nameIndex = result.indexOf("\t\tname: varchar(100)");

      assertTrue(idIndex < separatorIndex, "PK column must appear before the -- separator");
      assertTrue(separatorIndex < nameIndex, "Non-PK column must appear after the -- separator");
    }

    @Test
    void itDoesNotRenderSeparatorWhenAllColumnsAreInPrimaryKey() {
      final var idColumn =
          Column.builder()
              .isNullable(false)
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var primaryKey =
          PrimaryKey.builder().constraintName("pk_orders").columnNames(List.of("id")).build();
      final var table =
          Table.builder()
              .name("orders")
              .columns(List.of(idColumn))
              .primaryKey(primaryKey)
              .foreignKeys(List.of())
              .build();

      when(lineFormatter.format(any(Table.class), any(Column.class), isNull()))
          .thenReturn("id: uuid");

      final var result = entityRenderer.render(table);

      assertFalse(
          result.contains("--"), "Separator must not appear when there are no non-PK columns");
    }

    @Test
    void itDoesNotRenderSeparatorWhenNoPrimaryKeyIsSet() {
      final var nameColumn =
          Column.builder()
              .isNullable(false)
              .name("name")
              .dataType("varchar")
              .maximumLength(100)
              .constraints(List.of())
              .build();
      final var table =
          Table.builder().name("tags").columns(List.of(nameColumn)).foreignKeys(List.of()).build();

      when(lineFormatter.format(any(Table.class), any(Column.class), isNull()))
          .thenReturn("name: varchar(100)");

      final var result = entityRenderer.render(table);

      assertFalse(result.contains("--"), "Separator must not appear when there is no primary key");
    }
  }
}
