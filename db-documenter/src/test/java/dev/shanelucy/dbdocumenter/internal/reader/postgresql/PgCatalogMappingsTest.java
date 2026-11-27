package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.catalog.DataType;
import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.ReferentialAction;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TypeRow;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PgCatalogMappingsTest {

  @Nested
  class ReferentialActionTests {

    static Stream<Arguments> actionCodes() {
      return Stream.of(
          Arguments.of('a', ReferentialAction.NO_ACTION),
          Arguments.of('r', ReferentialAction.RESTRICT),
          Arguments.of('c', ReferentialAction.CASCADE),
          Arguments.of('n', ReferentialAction.SET_NULL),
          Arguments.of('d', ReferentialAction.SET_DEFAULT));
    }

    @ParameterizedTest
    @MethodSource("actionCodes")
    void itMapsEveryPostgresqlActionCode(final char code, final ReferentialAction expected) {
      assertEquals(expected, PgCatalogMappings.referentialAction(code));
    }

    @ParameterizedTest
    @ValueSource(chars = {'x', 'A', ' ', 'p'})
    void itThrowsExceptionWhenCodeIsUnknown(final char code) {
      final var exception =
          assertThrows(
              IllegalArgumentException.class, () -> PgCatalogMappings.referentialAction(code));

      assertEquals(
          "Unknown PostgreSQL referential action code '" + code + "'", exception.getMessage());
    }
  }

  @Nested
  class DataTypeTests {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "integer|integer",
          "character varying(20)[]|character varying(20)[]",
          "\"char\"|\"char\"",
          "numeric(10,2)|numeric(10,2)",
          "pg_catalog.int4|int4",
          "pg_catalog.\"char\"[]|\"char\"[]",
          "\"pg_catalog\".\"text\"[]|text[]",
          "\"uuid\"|uuid"
        })
    void itUsesFormatTypeWithoutSystemSchemaPrefixForPgCatalogTypes(
        final String formattedName, final String expectedDisplayText) {
      final var type = new TypeRow(formattedName, "pg_catalog", "ignored", null, null);

      assertEquals(
          DataType.builder().displayText(expectedDisplayText).build(),
          PgCatalogMappings.dataType(type));
    }

    @Test
    void itUsesFormatTypeForPgCatalogArrayEvenWhenElementIsKnown() {
      final var type = new TypeRow("integer[]", "pg_catalog", "_int4", "pg_catalog", "int4");

      assertEquals(
          DataType.builder().displayText("integer[]").build(), PgCatalogMappings.dataType(type));
    }

    @Test
    void itUsesElementNameWithBracketsAndElementAsUserTypeForUserArray() {
      final var type =
          new TypeRow("external_ref.priority_level[]", "core", "_Mood", "external_ref", "Mood");

      assertEquals(
          DataType.builder()
              .displayText("Mood[]")
              .userType(QualifiedName.of("external_ref", "Mood"))
              .build(),
          PgCatalogMappings.dataType(type));
    }

    @Test
    void itUsesRawTypeNameAndTypeAsUserTypeForEnumOrDomain() {
      final var type = new TypeRow("\"Mood\"", "core", "Mood", null, null);

      assertEquals(
          DataType.builder().displayText("Mood").userType(QualifiedName.of("core", "Mood")).build(),
          PgCatalogMappings.dataType(type));
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = "NULL",
        value = {"external_ref,NULL", "NULL,priority_level"})
    void whenOnlyOneElementFieldIsPresentTreatsTypeAsNonArray(
        final String elementSchema, final String elementName) {
      final var type = new TypeRow("ignored", "core", "_odd", elementSchema, elementName);

      assertEquals(
          DataType.builder().displayText("_odd").userType(QualifiedName.of("core", "_odd")).build(),
          PgCatalogMappings.dataType(type));
    }
  }
}
