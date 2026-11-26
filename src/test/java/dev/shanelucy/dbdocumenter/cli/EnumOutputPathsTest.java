package dev.shanelucy.dbdocumenter.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnumOutputPathsTest {

  @ParameterizedTest
  @CsvSource({
    "schema.puml, schema_enums.puml",
    "out, out_enums",
    "a.b.puml, a.b_enums.puml",
    ".puml, .puml_enums",
    "dir.v2/out, dir.v2/out_enums",
    "relative/dir/schema.puml, relative/dir/schema_enums.puml",
    "/abs/dir/schema.puml, /abs/dir/schema_enums.puml",
    "/abs.dir/out, /abs.dir/out_enums",
    "../up/schema.puml, ../up/schema_enums.puml"
  })
  void itAppendsSuffixToFileNameBeforeExtension(final String erdPath, final String expected) {
    final var result = EnumOutputPaths.derive(Path.of(erdPath));

    assertEquals(Path.of(expected), result);
  }

  @Test
  void whenPathHasNoParentResultIsRelative() {
    final var result = EnumOutputPaths.derive(Path.of("schema.puml"));

    assertEquals(Path.of("schema_enums.puml"), result);
    assertNull(result.getParent());
  }

  @Test
  void itRejectsPathWithoutFileName() {
    final var root = Path.of("/");

    final var exception =
        assertThrows(ValidationException.class, () -> EnumOutputPaths.derive(root));

    assertEquals("erdPath file name must not be null", exception.getMessage());
  }
}
