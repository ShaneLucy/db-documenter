package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PrimaryKeyTest {

  @Nested
  class ConstructorTests {

    static Stream<Arguments> invalidColumns() {
      return Stream.of(
          Arguments.of(null, "columns must contain at least 1 item"),
          Arguments.of(List.of(), "columns must contain at least 1 item"),
          Arguments.of(Arrays.asList("id", null), "columns must not contain null elements"),
          Arguments.of(List.of("id", ""), "columns must not be empty"),
          Arguments.of(
              List.of("tenant_id", "id", "tenant_id"),
              "columns must not contain duplicates, found duplicate: tenant_id"));
    }

    @Test
    void itKeepsColumnsInKeyOrder() {
      final var primaryKey = new PrimaryKey(Optional.of("pk"), List.of("tenant_id", "id"));

      assertEquals(List.of("tenant_id", "id"), primaryKey.columns());
      assertEquals(Optional.of("pk"), primaryKey.name());
    }

    @Test
    void itAcceptsAbsentName() {
      final var primaryKey = new PrimaryKey(Optional.empty(), List.of("id"));

      assertTrue(primaryKey.name().isEmpty());
    }

    @Test
    void itAcceptsWhitespaceNames() {
      final var primaryKey = new PrimaryKey(Optional.of(" "), List.of(" "));

      assertEquals(Optional.of(" "), primaryKey.name());
      assertEquals(List.of(" "), primaryKey.columns());
    }

    @Test
    void itThrowsExceptionWhenNameOptionalIsNull() {
      final var columns = List.of("id");

      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new PrimaryKey(null, columns));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsEmpty() {
      final var name = Optional.of("");
      final var columns = List.of("id");

      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new PrimaryKey(name, columns));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidColumns")
    void itThrowsExceptionWhenColumnsAreInvalid(
        final List<String> columns, final String expectedMessage) {
      final var exception =
          assertThrows(
              IllegalArgumentException.class, () -> new PrimaryKey(Optional.empty(), columns));

      assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void itDefensivelyCopiesColumns() {
      final var columns = new ArrayList<>(List.of("id"));
      final var primaryKey = new PrimaryKey(Optional.empty(), columns);

      columns.add("tenant_id");

      assertEquals(List.of("id"), primaryKey.columns());
    }

    @Test
    void itReturnsUnmodifiableColumns() {
      final var primaryKey = new PrimaryKey(Optional.empty(), new ArrayList<>(List.of("id")));
      final var columns = primaryKey.columns();

      assertThrows(UnsupportedOperationException.class, () -> columns.add("tenant_id"));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsPrimaryKeyWithName() {
      final var primaryKey = PrimaryKey.builder().name("users_pkey").columns(List.of("id")).build();

      assertEquals(new PrimaryKey(Optional.of("users_pkey"), List.of("id")), primaryKey);
    }

    @Test
    void itDefaultsNameToAbsent() {
      final var primaryKey = PrimaryKey.builder().columns(List.of("id")).build();

      assertTrue(primaryKey.name().isEmpty());
    }

    @Test
    void itTreatsNullNameAsAbsent() {
      final var primaryKey = PrimaryKey.builder().name(null).columns(List.of("id")).build();

      assertTrue(primaryKey.name().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenColumnsAreNotSet() {
      final var builder = PrimaryKey.builder().name("users_pkey");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("columns must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenBuiltWithEmptyName() {
      final var builder = PrimaryKey.builder().name("").columns(List.of("id"));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be empty", exception.getMessage());
    }
  }
}
