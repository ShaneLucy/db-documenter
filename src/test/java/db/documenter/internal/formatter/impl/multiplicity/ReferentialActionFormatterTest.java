package db.documenter.internal.formatter.impl.multiplicity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.ReferentialAction;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReferentialActionFormatterTest {

  private ReferentialActionFormatter referentialActionFormatter;

  @BeforeEach
  void setUp() {
    referentialActionFormatter = new ReferentialActionFormatter();
  }

  @Nested
  class FormatTests {

    @Test
    void whenBothActionsAreNoActionReturnsCurrent() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders", result);
    }

    @Test
    void whenOnlyOnDeleteIsSetAppendsOnDeleteLabel() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.CASCADE)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON DELETE CASCADE\"", result);
    }

    @Test
    void whenOnlyOnUpdateIsSetAppendsOnUpdateLabel() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onUpdateAction(ReferentialAction.CASCADE)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON UPDATE CASCADE\"", result);
    }

    @Test
    void whenBothActionsAreSetAppendsBothLabelsJoinedWithSlash() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.CASCADE)
              .onUpdateAction(ReferentialAction.RESTRICT)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON DELETE CASCADE / ON UPDATE RESTRICT\"", result);
    }

    @Test
    void whenOnDeleteIsSetNullRendersWithSpace() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.SET_NULL)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON DELETE SET NULL\"", result);
    }

    @Test
    void whenOnDeleteIsSetDefaultRendersWithSpace() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.SET_DEFAULT)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON DELETE SET DEFAULT\"", result);
    }

    @Test
    void whenOnDeleteIsRestrictRendersCorrectly() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.RESTRICT)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON DELETE RESTRICT\"", result);
    }

    @Test
    void whenBothActionsAreSetNullAppendsBothSetNullLabels() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.SET_NULL)
              .onUpdateAction(ReferentialAction.SET_NULL)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON DELETE SET NULL / ON UPDATE SET NULL\"", result);
    }

    @Test
    void whenCurrentSchemaNameDiffersOutputIsUnaffected() {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.CASCADE)
              .build();

      final var resultWithMatchingSchema =
          referentialActionFormatter.format(fk, "public", "users ||--|{ orders");
      final var resultWithDifferentSchema =
          referentialActionFormatter.format(fk, "other_schema", "users ||--|{ orders");

      assertEquals(resultWithMatchingSchema, resultWithDifferentSchema);
    }

    @ParameterizedTest
    @MethodSource(
        "db.documenter.internal.formatter.impl.multiplicity.ReferentialActionFormatterTest#nonNoActionDeleteValues")
    void whenOnDeleteActionIsSetRendersCorrectDisplayLabel(
        final ReferentialAction action, final String expectedLabel) {
      final var fk =
          ForeignKey.builder()
              .name("fk_test")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(action)
              .build();

      final var result = referentialActionFormatter.format(fk, "public", "users ||--|{ orders");

      assertEquals("users ||--|{ orders : \"ON DELETE " + expectedLabel + "\"", result);
    }
  }

  static Stream<Arguments> nonNoActionDeleteValues() {
    return Stream.of(
        Arguments.of(ReferentialAction.CASCADE, "CASCADE"),
        Arguments.of(ReferentialAction.RESTRICT, "RESTRICT"),
        Arguments.of(ReferentialAction.SET_NULL, "SET NULL"),
        Arguments.of(ReferentialAction.SET_DEFAULT, "SET DEFAULT"));
  }
}
