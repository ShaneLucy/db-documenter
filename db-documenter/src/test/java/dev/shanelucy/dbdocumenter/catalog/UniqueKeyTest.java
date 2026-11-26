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

class UniqueKeyTest {

  @Nested
  class ConstructorTests {

    static Stream<Arguments> invalidColumns() {
      return Stream.of(
          Arguments.of(null, "columns must contain at least 1 item"),
          Arguments.of(List.of(), "columns must contain at least 1 item"),
          Arguments.of(Arrays.asList(null, "email"), "columns must not contain null elements"),
          Arguments.of(List.of(""), "columns must not be empty"),
          Arguments.of(
              List.of("email", "email"),
              "columns must not contain duplicates, found duplicate: email"));
    }

    @Test
    void itKeepsColumnsInKeyOrder() {
      final var uniqueKey = new UniqueKey(Optional.of("uq"), List.of("tenant_id", "email"));

      assertEquals(List.of("tenant_id", "email"), uniqueKey.columns());
      assertEquals(Optional.of("uq"), uniqueKey.name());
    }

    @Test
    void itAcceptsAbsentName() {
      final var uniqueKey = new UniqueKey(Optional.empty(), List.of("email"));

      assertTrue(uniqueKey.name().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenNameOptionalIsNull() {
      final var columns = List.of("email");

      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new UniqueKey(null, columns));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsEmpty() {
      final var name = Optional.of("");
      final var columns = List.of("email");

      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new UniqueKey(name, columns));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidColumns")
    void itThrowsExceptionWhenColumnsAreInvalid(
        final List<String> columns, final String expectedMessage) {
      final var exception =
          assertThrows(
              IllegalArgumentException.class, () -> new UniqueKey(Optional.empty(), columns));

      assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void itDefensivelyCopiesColumns() {
      final var columns = new ArrayList<>(List.of("email"));
      final var uniqueKey = new UniqueKey(Optional.empty(), columns);

      columns.set(0, "username");

      assertEquals(List.of("email"), uniqueKey.columns());
    }

    @Test
    void itReturnsUnmodifiableColumns() {
      final var uniqueKey = new UniqueKey(Optional.empty(), new ArrayList<>(List.of("email")));
      final var columns = uniqueKey.columns();

      assertThrows(UnsupportedOperationException.class, () -> columns.add("username"));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsUniqueKeyWithName() {
      final var uniqueKey =
          UniqueKey.builder().name("users_email_key").columns(List.of("email")).build();

      assertEquals(new UniqueKey(Optional.of("users_email_key"), List.of("email")), uniqueKey);
    }

    @Test
    void itDefaultsNameToAbsent() {
      final var uniqueKey = UniqueKey.builder().columns(List.of("email")).build();

      assertTrue(uniqueKey.name().isEmpty());
    }

    @Test
    void itTreatsNullNameAsAbsent() {
      final var uniqueKey = UniqueKey.builder().name(null).columns(List.of("email")).build();

      assertTrue(uniqueKey.name().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenColumnsAreNotSet() {
      final var builder = UniqueKey.builder().name("users_email_key");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("columns must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenBuiltWithEmptyColumns() {
      final var builder = UniqueKey.builder().columns(List.of());

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("columns must contain at least 1 item", exception.getMessage());
    }
  }
}
