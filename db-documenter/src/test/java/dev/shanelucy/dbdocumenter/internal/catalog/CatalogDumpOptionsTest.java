package dev.shanelucy.dbdocumenter.internal.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.internal.catalog.CatalogDumpOptions.Mode;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CatalogDumpOptionsTest {

  @Nested
  class ConstructorTests {

    @Test
    void itThrowsExceptionWhenModeIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> new CatalogDumpOptions(null));

      assertEquals("mode must not be null", exception.getMessage());
    }
  }

  @Nested
  class VerbatimTests {

    @Test
    void itReturnsVerbatimMode() {
      assertEquals(Mode.VERBATIM, CatalogDumpOptions.verbatim().mode());
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsOptionsWithMode() {
      final var options = CatalogDumpOptions.builder().mode(Mode.VERBATIM).build();

      assertEquals(CatalogDumpOptions.verbatim(), options);
    }

    @Test
    void itThrowsExceptionWhenModeIsUnset() {
      final var builder = CatalogDumpOptions.builder();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("mode must not be null", exception.getMessage());
    }
  }
}
