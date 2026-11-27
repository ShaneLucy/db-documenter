package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ColumnTest {

  private static final DataType INT4 = DataType.builder().displayText("int4").build();

  @Nested
  class ConstructorTests {

    @ParameterizedTest
    @ValueSource(strings = {" ", "my column", "Id"})
    void itAcceptsNamesWithWhitespaceOrMixedCase(final String name) {
      final var column = new Column(name, true, INT4);

      assertEquals(name, column.name());
    }

    @Test
    void itThrowsExceptionWhenNameIsNull() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new Column(null, true, INT4));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsEmpty() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new Column("", true, INT4));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenDataTypeIsNull() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new Column("id", true, null));

      assertEquals("dataType must not be null", exception.getMessage());
    }
  }

  @Nested
  class BuilderTests {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void itBuildsColumnWithGivenNullability(final boolean nullable) {
      final var column = Column.builder().name("id").nullable(nullable).dataType(INT4).build();

      assertEquals(new Column("id", nullable, INT4), column);
    }

    @Test
    void itThrowsExceptionWhenNameIsNotSet() {
      final var builder = Column.builder().nullable(false).dataType(INT4);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itDefaultsToNullableWhenNullableIsNotSet() {
      final var column = Column.builder().name("id").dataType(INT4).build();

      assertEquals(new Column("id", true, INT4), column);
    }

    @Test
    void itThrowsExceptionWhenDataTypeIsNotSet() {
      final var builder = Column.builder().name("id").nullable(false);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("dataType must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenBuiltWithEmptyName() {
      final var builder = Column.builder().name("").nullable(false).dataType(INT4);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be empty", exception.getMessage());
    }
  }
}
