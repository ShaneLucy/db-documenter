package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnumTypeTest {

  private static final QualifiedName MOOD = QualifiedName.of("app", "mood");

  @Nested
  class ConstructorTests {

    @Test
    void itKeepsValuesInDeclaredOrder() {
      final var enumType = new EnumType(MOOD, List.of("sad", "ok", "happy", "Angry"));

      assertEquals(List.of("sad", "ok", "happy", "Angry"), enumType.values());
    }

    @Test
    void itAcceptsEmptyAndWhitespaceValues() {
      final var enumType = new EnumType(MOOD, List.of("", " "));

      assertEquals(List.of("", " "), enumType.values());
    }

    @Test
    void itThrowsExceptionWhenNameIsNull() {
      final var values = List.of("sad");

      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new EnumType(null, values));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenValuesIsNull() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new EnumType(MOOD, null));

      assertEquals("values must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenValuesContainNull() {
      final var values = Arrays.asList("sad", null);

      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new EnumType(MOOD, values));

      assertEquals("values must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenValuesContainDuplicate() {
      final var values = List.of("sad", "ok", "sad");

      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new EnumType(MOOD, values));

      assertEquals(
          "values must not contain duplicates, found duplicate: sad", exception.getMessage());
    }

    @Test
    void itDefensivelyCopiesValues() {
      final var values = new ArrayList<>(List.of("sad", "ok"));
      final var enumType = new EnumType(MOOD, values);

      values.add("happy");

      assertEquals(List.of("sad", "ok"), enumType.values());
    }

    @Test
    void itReturnsUnmodifiableValues() {
      final var enumType = new EnumType(MOOD, new ArrayList<>(List.of("sad")));
      final var values = enumType.values();

      assertThrows(UnsupportedOperationException.class, () -> values.add("ok"));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsEnumType() {
      final var enumType = EnumType.builder().name(MOOD).values(List.of("sad", "ok")).build();

      assertEquals(new EnumType(MOOD, List.of("sad", "ok")), enumType);
    }

    @Test
    void itDefaultsValuesToEmpty() {
      final var enumType = EnumType.builder().name(MOOD).build();

      assertTrue(enumType.values().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenNameIsNotSet() {
      final var builder = EnumType.builder().values(List.of("sad"));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be null", exception.getMessage());
    }
  }
}
