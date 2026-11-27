package dev.shanelucy.dbdocumenter.internal.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class IdentifiersTest {

  private static String codePoint(final int codePoint) {
    return new String(Character.toChars(codePoint));
  }

  @Nested
  class FormatTests {

    static Stream<Arguments> quotedIdentifiers() {
      return Stream.of(
          Arguments.of("Users", "\"Users\""),
          Arguments.of("my table", "\"my table\""),
          Arguments.of("a.b", "\"a.b\""),
          Arguments.of("a-b", "\"a-b\""),
          Arguments.of("é", "\"é\""),
          Arguments.of("", "\"\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"users", "a1_", "123", "_"})
    void itLeavesLowercaseAlphanumericIdentifiersBare(final String identifier) {
      assertEquals(identifier, Identifiers.format(identifier));
    }

    @ParameterizedTest
    @MethodSource("quotedIdentifiers")
    void itQuotesEveryOtherIdentifier(final String identifier, final String expected) {
      assertEquals(expected, Identifiers.format(identifier));
    }

    @Test
    void itEscapesSpecialCharactersInQuotedIdentifiers() {
      assertEquals("\"a\\\"b\\nc\"", Identifiers.format("a\"b\nc"));
    }
  }

  @Nested
  class QuoteTests {

    static Stream<Arguments> escapedCodePoints() {
      return Stream.of(
          Arguments.of((int) '\n', "\\n"),
          Arguments.of((int) '\r', "\\r"),
          Arguments.of((int) '\t', "\\t"),
          Arguments.of((int) '\\', "\\\\"),
          Arguments.of((int) '"', "\\\""),
          Arguments.of(0x0000, "\\u0000"),
          Arguments.of(0x007F, "\\u007F"),
          Arguments.of(0x0085, "\\u0085"),
          Arguments.of(0x200B, "\\u200B"),
          Arguments.of(0x202E, "\\u202E"),
          Arguments.of(0x2028, "\\u2028"),
          Arguments.of(0x2029, "\\u2029"),
          Arguments.of(0xE000, "\\uE000"),
          Arguments.of(0xD800, "\\uD800"),
          Arguments.of(0xDC00, "\\uDC00"),
          Arguments.of(0xE0001, "\\uDB40\\uDC01"));
    }

    @ParameterizedTest
    @MethodSource("escapedCodePoints")
    void itEscapesUnprintableAndSpecialCharacters(final int codePoint, final String expected) {
      assertEquals('"' + expected + '"', Identifiers.quote(codePoint(codePoint)));
    }

    @ParameterizedTest
    @ValueSource(ints = {' ', 0x1F600, 0x00E9})
    void itPrintsPrintableCharactersAsThemselves(final int codePoint) {
      final var value = codePoint(codePoint);

      assertEquals('"' + value + '"', Identifiers.quote(value));
    }

    @Test
    void itEscapesOnlyTheSpecialCharactersWithinAValue() {
      assertEquals("\"a b\\tc\\u200Bd\"", Identifiers.quote("a b\tc​d"));
    }

    @Test
    void itQuotesAnEmptyValue() {
      assertEquals("\"\"", Identifiers.quote(""));
    }

    @Test
    void itQuotesEvenABareIdentifier() {
      assertEquals("\"users\"", Identifiers.quote("users"));
      assertEquals("users", Identifiers.format("users"));
    }
  }

  @Nested
  class FormatQualifiedNameTests {

    static Stream<Arguments> qualifiedNames() {
      return Stream.of(
          Arguments.of(QualifiedName.of("app", "users"), "app.users"),
          Arguments.of(QualifiedName.of("main", "Mixed Case"), "main.\"Mixed Case\""),
          Arguments.of(QualifiedName.of("a.b", "c"), "\"a.b\".c"),
          Arguments.of(QualifiedName.of("a", "b.c"), "a.\"b.c\""));
    }

    @ParameterizedTest
    @MethodSource("qualifiedNames")
    void itFormatsEachPartIndependently(final QualifiedName qualifiedName, final String expected) {
      assertEquals(expected, Identifiers.format(qualifiedName));
    }

    @Test
    void itDistinguishesADotInTheSchemaFromADotInTheName() {
      assertNotEquals(
          Identifiers.format(QualifiedName.of("a.b", "c")),
          Identifiers.format(QualifiedName.of("a", "b.c")));
    }
  }
}
