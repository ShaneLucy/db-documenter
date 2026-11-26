package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CatalogValidationTest {

  @Nested
  class ValidateColumnNamesTests {

    static Stream<Arguments> invalidColumns() {
      return Stream.of(
          Arguments.of(null, "cols must contain at least 1 item"),
          Arguments.of(List.of(), "cols must contain at least 1 item"),
          Arguments.of(Arrays.asList("a", null), "cols must not contain null elements"),
          Arguments.of(List.of("a", ""), "cols must not be empty"));
    }

    @Test
    void itAcceptsWhitespaceAndDuplicateNames() {
      assertDoesNotThrow(() -> CatalogValidation.validateColumnNames(List.of(" ", " "), "cols"));
    }

    @ParameterizedTest
    @MethodSource("invalidColumns")
    void itThrowsExceptionWhenColumnsAreInvalid(
        final List<String> columns, final String expectedMessage) {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> CatalogValidation.validateColumnNames(columns, "cols"));

      assertEquals(expectedMessage, exception.getMessage());
    }
  }

  @Nested
  class ValidateOptionalNameTests {

    @Test
    void itAcceptsAbsentName() {
      assertDoesNotThrow(() -> CatalogValidation.validateOptionalName(Optional.empty(), "name"));
    }

    @Test
    void itAcceptsWhitespaceName() {
      assertDoesNotThrow(() -> CatalogValidation.validateOptionalName(Optional.of(" "), "name"));
    }

    @Test
    void itThrowsExceptionWhenOptionalIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> CatalogValidation.validateOptionalName(null, "name"));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenPresentNameIsEmpty() {
      final var name = Optional.of("");

      final var exception =
          assertThrows(
              ValidationException.class,
              () -> CatalogValidation.validateOptionalName(name, "name"));

      assertEquals("name must not be empty", exception.getMessage());
    }
  }

  @Nested
  class ValidateUniqueKeysTests {

    @Test
    void itAcceptsDistinctKeys() {
      assertDoesNotThrow(
          () ->
              CatalogValidation.validateUniqueKeys(
                  List.of("apple", "banana"), (String value) -> value.charAt(0), "fruit"));
    }

    @Test
    void itAcceptsEmptyList() {
      assertDoesNotThrow(
          () -> CatalogValidation.validateUniqueKeys(List.of(), Function.identity(), "fruit"));
    }

    @Test
    void itThrowsExceptionNamingTheDuplicateKey() {
      final var elements = List.of("apple", "banana", "avocado");

      final var exception =
          assertThrows(
              ValidationException.class,
              () ->
                  CatalogValidation.validateUniqueKeys(
                      elements, (String value) -> value.charAt(0), "fruit.initial"));

      assertEquals(
          "fruit.initial must not contain duplicates, found duplicate: a", exception.getMessage());
    }
  }

  @Nested
  class ValidateColumnsExistTests {

    @Test
    void itAcceptsColumnsThatAreAllInTheTable() {
      assertDoesNotThrow(
          () ->
              CatalogValidation.validateColumnsExist(
                  List.of("id", "code"), Set.of("id", "code", "name"), "uniqueKeys"));
    }

    @Test
    void itThrowsExceptionNamingFirstUnknownColumn() {
      final var columns = List.of("id", "missing", "other");
      final var tableColumns = Set.of("id");

      final var exception =
          assertThrows(
              ValidationException.class,
              () -> CatalogValidation.validateColumnsExist(columns, tableColumns, "foreignKeys"));

      assertEquals(
          "foreignKeys refers to column missing, which is not in the table",
          exception.getMessage());
    }

    @Test
    void itMatchesColumnNamesCaseSensitively() {
      final var columns = List.of("ID");
      final var tableColumns = Set.of("id");

      final var exception =
          assertThrows(
              ValidationException.class,
              () -> CatalogValidation.validateColumnsExist(columns, tableColumns, "primaryKey"));

      assertEquals(
          "primaryKey refers to column ID, which is not in the table", exception.getMessage());
    }
  }
}
