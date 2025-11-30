package db.documenter.internal.formatter.impl.entity;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import java.util.List;
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
    table = Table.builder().name("test_table").columns(List.of()).foreignKeys(List.of()).build();
    columnBuilder =
        Column.builder().name("column_name").dataType("column_data_type").constraints(List.of());
  }

  @Nested
  class FormatTests {

    @Test
    void ifCurrentIsNotNullItReturnsCurrent() {
      final var currentValue = "some random current value";

      final var result =
          defaultEntityLineFormatter.format(
              table,
              Column.builder().name("col").dataType("type").constraints(List.of()).build(),
              currentValue);

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
    void ifColumnNameIsNullThrowsValidationException() {
      assertThrows(
          db.documenter.internal.exceptions.ValidationException.class,
          () -> Column.builder().name(null).dataType("type").maximumLength(10).build());
    }

    @Test
    void ifColumnDataTypeIsNullThrowsValidationException() {
      assertThrows(
          db.documenter.internal.exceptions.ValidationException.class,
          () -> Column.builder().name("col").dataType(null).maximumLength(10).build());
    }

    @Test
    void ifColumnMaximumLengthIsNegativeTreatsAsZero() {
      final var column =
          Column.builder()
              .name("col")
              .dataType("type")
              .maximumLength(-1)
              .constraints(List.of())
              .build();

      final var result = defaultEntityLineFormatter.format(table, column, null);

      assertEquals("col: type", result);
    }

    @Test
    void ifColumnMaximumLengthIsVeryLargeFormatsCorrectly() {
      final var column =
          Column.builder()
              .name("col")
              .dataType("type")
              .maximumLength(Integer.MAX_VALUE)
              .constraints(List.of())
              .build();

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
