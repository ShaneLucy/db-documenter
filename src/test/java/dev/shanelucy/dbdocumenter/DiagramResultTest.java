package db.documenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.exceptions.ValidationException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiagramResultTest {

  @Nested
  class ConstructorTests {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n\t"})
    void itThrowsExceptionWhenErdDiagramIsBlank(final String erd) {
      final var exception =
          assertThrows(ValidationException.class, () -> new DiagramResult(erd, Optional.empty()));

      assertEquals("erdDiagram must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumDiagramOptionalIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> new DiagramResult("@startuml", null));

      assertEquals("enumDiagram must not be null", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void itThrowsExceptionWhenPresentEnumDiagramIsBlank(final String enums) {
      final var exception =
          assertThrows(
              ValidationException.class, () -> new DiagramResult("@startuml", Optional.of(enums)));

      assertEquals("enumDiagram must not be blank", exception.getMessage());
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsResultWithEnumDiagram() {
      final var result = DiagramResult.builder().erdDiagram("erd").enumDiagram("enums").build();

      assertEquals("erd", result.erdDiagram());
      assertEquals(Optional.of("enums"), result.enumDiagram());
    }

    @Test
    void itBuildsResultWithoutEnumDiagram() {
      final var result = DiagramResult.builder().erdDiagram("erd").build();

      assertEquals("erd", result.erdDiagram());
      assertTrue(result.enumDiagram().isEmpty());
    }

    @Test
    void itTreatsNullEnumDiagramAsAbsent() {
      final var result = DiagramResult.builder().erdDiagram("erd").enumDiagram(null).build();

      assertTrue(result.enumDiagram().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenErdDiagramIsNotSet() {
      final var exception =
          assertThrows(ValidationException.class, () -> DiagramResult.builder().build());

      assertEquals("erdDiagram must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenBuiltWithBlankEnumDiagram() {
      final var builder = DiagramResult.builder().erdDiagram("erd").enumDiagram(" ");

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("enumDiagram must not be blank", exception.getMessage());
    }
  }
}
