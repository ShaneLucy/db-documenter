package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.internal.catalog.Identifiers;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class QualifiedNameTest {

  @Nested
  class ConstructorTests {

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "my table", "\n"})
    void itAcceptsWhitespaceInBothParts(final String value) {
      final var qualifiedName = new QualifiedName(value, value);

      assertEquals(value, qualifiedName.schema());
      assertEquals(value, qualifiedName.name());
    }

    @Test
    void itThrowsExceptionWhenSchemaIsNull() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new QualifiedName(null, "users"));

      assertEquals("schema must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemaIsEmpty() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new QualifiedName("", "users"));

      assertEquals("schema must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsNull() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new QualifiedName("app", null));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsEmpty() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new QualifiedName("app", ""));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itCreatesTheSameValueViaOf() {
      assertEquals(new QualifiedName("app", "users"), QualifiedName.of("app", "users"));
    }

    @Test
    void itThrowsExceptionWhenOfIsGivenAnEmptyName() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> QualifiedName.of("app", ""));

      assertEquals("name must not be empty", exception.getMessage());
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsQualifiedName() {
      final var qualifiedName = QualifiedName.builder().schema("app").name("users").build();

      assertEquals(QualifiedName.of("app", "users"), qualifiedName);
    }

    @Test
    void itThrowsExceptionWhenSchemaIsNotSet() {
      final var builder = QualifiedName.builder().name("users");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("schema must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsNotSet() {
      final var builder = QualifiedName.builder().schema("app");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenBuiltWithEmptySchema() {
      final var builder = QualifiedName.builder().schema("").name("users");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("schema must not be empty", exception.getMessage());
    }
  }

  @Nested
  class CompareToTests {

    static Stream<Arguments> orderedPairs() {
      return Stream.of(
          Arguments.of(QualifiedName.of("a", "z"), QualifiedName.of("b", "a")),
          Arguments.of(QualifiedName.of("app", "orders"), QualifiedName.of("app", "users")),
          Arguments.of(QualifiedName.of("App", "users"), QualifiedName.of("app", "users")),
          Arguments.of(QualifiedName.of("app", "user"), QualifiedName.of("app", "users")));
    }

    @ParameterizedTest
    @MethodSource("orderedPairs")
    void itOrdersBySchemaThenName(final QualifiedName smaller, final QualifiedName larger) {
      assertTrue(smaller.compareTo(larger) < 0, smaller + " should sort before " + larger);
      assertTrue(larger.compareTo(smaller) > 0, larger + " should sort after " + smaller);
    }

    @Test
    void itComparesEqualNamesAsZero() {
      assertEquals(0, QualifiedName.of("app", "users").compareTo(QualifiedName.of("app", "users")));
    }

    @Test
    void itSortsAListBySchemaThenName() {
      final var names =
          new ArrayList<>(
              List.of(
                  QualifiedName.of("b", "a"),
                  QualifiedName.of("a", "z"),
                  QualifiedName.of("a", "b")));

      names.sort(null);

      assertEquals(
          List.of(
              QualifiedName.of("a", "b"), QualifiedName.of("a", "z"), QualifiedName.of("b", "a")),
          names);
    }
  }

  @Nested
  class ToStringTests {

    static Stream<Arguments> qualifiedNames() {
      return Stream.of(
          Arguments.of(QualifiedName.of("app", "users"), "app.users"),
          Arguments.of(QualifiedName.of("main", "Mixed Case"), "main.\"Mixed Case\""));
    }

    @ParameterizedTest
    @MethodSource("qualifiedNames")
    void itFormatsBareAndQuotedParts(final QualifiedName qualifiedName, final String expected) {
      assertEquals(expected, qualifiedName.toString());
    }

    @Test
    void itDelegatesToIdentifiersFormat() {
      final var qualifiedName = QualifiedName.of("a.b", "C");

      assertEquals(Identifiers.format(qualifiedName), qualifiedName.toString());
    }
  }
}
