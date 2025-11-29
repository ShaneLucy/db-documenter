package db.documenter.internal.formatter.impl.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ForeignKeyEntityLineFormatterTest {

  private ForeignKeyEntityLineFormatter foreignKeyEntityLineFormatter;
  private Table.Builder tableBuilder;
  private Column.Builder columnBuilder;

  @BeforeEach
  void setUp() {
    foreignKeyEntityLineFormatter = new ForeignKeyEntityLineFormatter();
    tableBuilder = Table.builder();
    columnBuilder = Column.builder().name("col").dataType("varchar");
  }

  @Nested
  class FormatTests {

    @Test
    void ifNoForeignKeysReturnsCurrent() {
      final var table = tableBuilder.build();
      final var result =
          foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "value");
      assertEquals("value", result);
    }

    @Test
    void ifMatchingForeignKeyDecoratesCurrent() {
      final var fk =
          ForeignKey.builder()
              .sourceColumn("col")
              .targetTable("target_table")
              .targetColumn("target_col")
              .build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result =
          foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "value");

      assertEquals("__value__ '→ target_table.target_col", result);
    }

    @Test
    void matchingIsCaseInsensitive() {
      final var fk =
          ForeignKey.builder().sourceColumn("COL").targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "v");

      assertEquals("__v__ '→ t.c", result);
    }

    @Test
    void ifMultipleForeignKeysOnlyFirstMatchIsUsed() {
      final var fk1 =
          ForeignKey.builder().sourceColumn("col").targetTable("A").targetColumn("a").build();

      final var fk2 =
          ForeignKey.builder().sourceColumn("col").targetTable("B").targetColumn("b").build();

      final var table = tableBuilder.foreignKeys(List.of(fk1, fk2)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "x");

      assertEquals("__x__ '→ A.a", result);
    }

    @Test
    void ifColumnDoesNotMatchAnyForeignKeyReturnsCurrent() {
      final var fk =
          ForeignKey.builder().sourceColumn("different").targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "orig");

      assertEquals("orig", result);
    }

    @Test
    void ifCurrentIsNullItDecoratesNullValueTextually() {
      final var fk =
          ForeignKey.builder().sourceColumn("col").targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), null);

      assertEquals("__null__ '→ t.c", result);
    }

    @Test
    void ifCurrentIsEmptyStringDecoratesIt() {
      final var fk =
          ForeignKey.builder().sourceColumn("col").targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "");

      assertEquals("____ '→ t.c", result);
    }

    @Test
    void ifCurrentIsWhitespaceDecoratesIt() {
      final var fk =
          ForeignKey.builder().sourceColumn("col").targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "  ");

      assertEquals("__  __ '→ t.c", result);
    }

    @Test
    void ifForeignKeyHasNullTargetTableNullIsPrinted() {
      final var fk =
          ForeignKey.builder().sourceColumn("col").targetTable(null).targetColumn("x").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "v");

      assertEquals("__v__ '→ null.x", result);
    }

    @Test
    void ifForeignKeyHasNullTargetColumnNullIsPrinted() {
      final var fk =
          ForeignKey.builder().sourceColumn("col").targetTable("t").targetColumn(null).build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var result = foreignKeyEntityLineFormatter.format(table, columnBuilder.build(), "v");

      assertEquals("__v__ '→ t.null", result);
    }

    @Test
    void ifForeignKeySourceColumnIsNullItThrowsNullPointerException() {
      final var fk =
          ForeignKey.builder().sourceColumn(null).targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var column = columnBuilder.build();
      assertThrows(
          NullPointerException.class,
          () -> foreignKeyEntityLineFormatter.format(table, column, "v"));
    }

    @Test
    void ifColumnNameIsNullDoesNotMatchAnyForeignKeyReturnsCurrent() {
      final var fk =
          ForeignKey.builder().sourceColumn("col").targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      final var column = Column.builder().name(null).build();
      final var result = foreignKeyEntityLineFormatter.format(table, column, "v");

      assertEquals("v", result);
    }

    @Test
    void ifTableIsNullThrowsNullPointerException() {
      final var column = columnBuilder.build();
      assertThrows(
          NullPointerException.class,
          () -> foreignKeyEntityLineFormatter.format(null, column, "v"));
    }

    @Test
    void ifColumnIsNullThrowsNullPointerException() {
      final var fk =
          ForeignKey.builder().sourceColumn("col").targetTable("t").targetColumn("c").build();

      final var table = tableBuilder.foreignKeys(List.of(fk)).build();
      assertThrows(
          NullPointerException.class, () -> foreignKeyEntityLineFormatter.format(table, null, "v"));
    }
  }
}
