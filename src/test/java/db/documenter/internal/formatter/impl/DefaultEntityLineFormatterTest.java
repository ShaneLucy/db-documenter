package db.documenter.internal.formatter.impl;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultEntityLineFormatterTest {

  private DefaultEntityLineFormatter defaultEntityLineFormatter;
  private Table table;
  private Column.Builder columnBuilder;

  @BeforeEach
  void setUp() {
    defaultEntityLineFormatter = new DefaultEntityLineFormatter();
    table = Table.builder().build();
    columnBuilder = Column.builder().name("column_name").dataType("column_data_type");
  }

  @Nested
  class FormatTests {

    @Test
    void ifCurrentIsNotNullItReturnsCurrent() {
      final var currentValue = "some random current value";

      final var result =
          defaultEntityLineFormatter.format(table, Column.builder().build(), currentValue);

      assertEquals(currentValue, result);
    }

    @Test
    void ifColumnMaximumLengthGreaterThan0ThenReturnStringIsFormattedCorrectly() {
      columnBuilder.maximumLength(123);
      final var column = columnBuilder.build();

      final var result = defaultEntityLineFormatter.format(table, column, null);

      assertEquals(
          String.format("%s: %s(%d)", column.name(), column.dataType(), column.maximumLength()),
          result);
    }

    @Test
    void ifColumnMaximumLengthEquals0ThenReturnStringIsFormattedCorrectly() {
      final var column = columnBuilder.build();

      final var result = defaultEntityLineFormatter.format(table, column, null);

      assertEquals(String.format("%s: %s", column.name(), column.dataType()), result);
    }

    @Test
    void ifTableIsNullItDoesNotThrowException() {
      assertDoesNotThrow(
          () -> defaultEntityLineFormatter.format(null, columnBuilder.build(), "current"));
    }

    @Test
    void ifCurrentIsNullItDoesNotThrowException() {
      assertDoesNotThrow(
          () -> defaultEntityLineFormatter.format(table, columnBuilder.build(), null));
    }

    @Test
    void ifCurrentIsEmptyStringItReturnsEmptyString() {
      final var result = defaultEntityLineFormatter.format(table, columnBuilder.build(), "");
      assertEquals("", result);
    }

    @Test
    void ifCurrentIsWhitespaceItReturnsWhitespace() {
      final var result = defaultEntityLineFormatter.format(table, columnBuilder.build(), "   ");
      assertEquals("   ", result);
    }

    @Test
    void ifColumnNameIsNullDoesNotThrow() {
      final var column = Column.builder().name(null).dataType("type").maximumLength(10).build();

      assertDoesNotThrow(() -> defaultEntityLineFormatter.format(table, column, null));
    }

    @Test
    void ifColumnDataTypeIsNullDoesNotThrow() {
      final var column = Column.builder().name("col").dataType(null).maximumLength(10).build();

      assertDoesNotThrow(() -> defaultEntityLineFormatter.format(table, column, null));
    }

    @Test
    void ifColumnMaximumLengthIsNegativeTreatsAsZero() {
      final var column = Column.builder().name("col").dataType("type").maximumLength(-1).build();

      final var result = defaultEntityLineFormatter.format(table, column, null);

      assertEquals("col: type", result);
    }

    @Test
    void ifColumnMaximumLengthIsVeryLargeFormatsCorrectly() {
      final var column =
          Column.builder().name("col").dataType("type").maximumLength(Integer.MAX_VALUE).build();

      final var result = defaultEntityLineFormatter.format(table, column, null);

      assertEquals(String.format("col: type(%d)", Integer.MAX_VALUE), result);
    }

    @Test
    void ifColumnIsNullThrowsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> defaultEntityLineFormatter.format(table, null, null));
    }

    @Test
    void ifTableIsNullAndCurrentIsNullDoesNotThrow() {
      final var column = columnBuilder.maximumLength(10).build();
      assertDoesNotThrow(() -> defaultEntityLineFormatter.format(null, column, null));
    }
  }
}
