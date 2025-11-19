package db.documenter.internal.formatter.impl;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NullableLineFormatterTest {

  private NullableLineFormatter nullableLineFormatter;
  private Column.Builder columnBuilder;
  private Table table;

  @BeforeEach
  void setUp() {
    nullableLineFormatter = new NullableLineFormatter();
    table = Table.builder().build();
    columnBuilder = Column.builder().name("col").dataType("varchar");
  }

  @Nested
  class FormatTests {

    @Test
    void ifColumnIsNullableAppendsQuestionMark() {
      final var column = columnBuilder.isNullable(true).build();
      final var result = nullableLineFormatter.format(table, column, "value");
      assertEquals("value?", result);
    }

    @Test
    void ifColumnIsNotNullableReturnsCurrentAsIs() {
      final var column = columnBuilder.isNullable(false).build();
      final var result = nullableLineFormatter.format(table, column, "value");
      assertEquals("value", result);
    }

    @Test
    void ifCurrentIsNullAndColumnIsNullableAppendsQuestionMark() {
      final var column = columnBuilder.isNullable(true).build();
      final var result = nullableLineFormatter.format(table, column, null);
      assertEquals("null?", result);
    }

    @Test
    void ifCurrentIsNullAndColumnIsNotNullableReturnsNull() {
      final var column = columnBuilder.isNullable(false).build();
      final var result = nullableLineFormatter.format(table, column, null);
      assertNull(result);
    }

    @Test
    void ifCurrentIsEmptyStringAndColumnIsNullableAppendsQuestionMark() {
      final var column = columnBuilder.isNullable(true).build();
      final var result = nullableLineFormatter.format(table, column, "");
      assertEquals("?", result);
    }

    @Test
    void ifCurrentIsWhitespaceAndColumnIsNullableAppendsQuestionMark() {
      final var column = columnBuilder.isNullable(true).build();
      final var result = nullableLineFormatter.format(table, column, "  ");
      assertEquals("  ?", result);
    }

    @Test
    void ifColumnIsNullThrowsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> nullableLineFormatter.format(table, null, "value"));
    }

    @Test
    void ifTableIsNullDoesNotThrow() {
      final var column = columnBuilder.isNullable(true).build();
      assertDoesNotThrow(() -> nullableLineFormatter.format(null, column, "value"));
    }
  }
}
