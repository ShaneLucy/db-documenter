package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConstraintTypeTest {

  @Nested
  class FromCodeTests {

    @ParameterizedTest
    @CsvSource({"p,PRIMARY_KEY", "u,UNIQUE", "f,FOREIGN_KEY"})
    void itMapsEveryReadContypeCode(final char code, final ConstraintType expected) {
      assertEquals(expected, ConstraintType.fromCode(code));
    }

    @ParameterizedTest
    @EnumSource(ConstraintType.class)
    void itRoundTripsThroughCode(final ConstraintType type) {
      assertEquals(type, ConstraintType.fromCode(type.code()));
    }

    // c (check), t (trigger), x (exclusion) and n (not-null, PG 18) are real codes the query
    // filters out; the rest are not codes at all.
    @ParameterizedTest
    @ValueSource(chars = {'c', 't', 'x', 'n', 'P', ' '})
    void itThrowsExceptionWhenCodeIsUnknown(final char code) {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> ConstraintType.fromCode(code));

      assertEquals("Unknown PostgreSQL constraint type '" + code + "'", exception.getMessage());
    }
  }
}
