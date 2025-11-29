package db.documenter.internal.formatter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PrimaryKeyEntityLineFormatterTest {

  private PrimaryKeyEntityLineFormatter primaryKeyEntityLineFormatter;
  private Column.Builder columnBuilder;
  private Table.Builder tableBuilder;

  @BeforeEach
  void setUp() {
    primaryKeyEntityLineFormatter = new PrimaryKeyEntityLineFormatter();
    columnBuilder = Column.builder().name("col").dataType("varchar");
    tableBuilder = Table.builder();
  }

  @Nested
  class FormatTests {

    @Test
    void ifColumnIsPrimaryKeyReturnsBoldedCurrent() {
      final var column = columnBuilder.build();
      final var pk = PrimaryKey.builder().columnNames(List.of("col")).build();
      final var table = tableBuilder.primaryKey(pk).build();

      final var result = primaryKeyEntityLineFormatter.format(table, column, "value");

      assertEquals("**value**", result);
    }

    @Test
    void ifColumnIsNotPrimaryKeyReturnsCurrentAsIs() {
      final var column = columnBuilder.build();
      final var pk = PrimaryKey.builder().columnNames(List.of("other")).build();
      final var table = tableBuilder.primaryKey(pk).build();

      final var result = primaryKeyEntityLineFormatter.format(table, column, "value");

      assertEquals("value", result);
    }

    @Test
    void ifTableHasNoPrimaryKeyReturnsCurrent() {
      final var column = columnBuilder.build();
      final var table = tableBuilder.build();

      final var result = primaryKeyEntityLineFormatter.format(table, column, "value");

      assertEquals("value", result);
    }

    @Test
    void ifCurrentIsNullAndColumnIsPrimaryKeyReturnsBoldedNull() {
      final var column = columnBuilder.build();
      final var pk = PrimaryKey.builder().columnNames(List.of("col")).build();
      final var table = tableBuilder.primaryKey(pk).build();

      final var result = primaryKeyEntityLineFormatter.format(table, column, null);

      assertEquals("**null**", result);
    }

    @Test
    void ifCurrentIsEmptyStringAndColumnIsPrimaryKeyReturnsBoldedEmpty() {
      final var column = columnBuilder.build();
      final var pk = PrimaryKey.builder().columnNames(List.of("col")).build();
      final var table = tableBuilder.primaryKey(pk).build();

      final var result = primaryKeyEntityLineFormatter.format(table, column, "");

      assertEquals("****", result);
    }

    @Test
    void ifCurrentIsWhitespaceAndColumnIsPrimaryKeyReturnsBoldedWhitespace() {
      final var column = columnBuilder.build();
      final var pk = PrimaryKey.builder().columnNames(List.of("col")).build();
      final var table = tableBuilder.primaryKey(pk).build();

      final var result = primaryKeyEntityLineFormatter.format(table, column, "  ");

      assertEquals("**  **", result);
    }

    @Test
    void ifColumnIsNullThrowsNullPointerException() {
      final var table = tableBuilder.build();
      assertThrows(
          NullPointerException.class,
          () -> primaryKeyEntityLineFormatter.format(table, null, "value"));
    }

    @Test
    void ifTableIsNullThrowsNullPointerException() {
      final var column = columnBuilder.build();
      assertThrows(
          NullPointerException.class,
          () -> primaryKeyEntityLineFormatter.format(null, column, "value"));
    }

    @Test
    void ifColumnIsPrimaryKeyAndCurrentIsWhitespaceOnly() {
      final var column = columnBuilder.build();
      final var pk = PrimaryKey.builder().columnNames(List.of("col")).build();
      final var table = tableBuilder.primaryKey(pk).build();

      final var result = primaryKeyEntityLineFormatter.format(table, column, "   ");

      assertEquals("**   **", result);
    }
  }
}
