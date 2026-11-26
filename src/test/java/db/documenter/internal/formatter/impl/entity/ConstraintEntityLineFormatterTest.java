package db.documenter.internal.formatter.impl.entity;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConstraintEntityLineFormatterTest {

  private ConstraintEntityLineFormatter constraintEntityLineFormatter;
  private Column.Builder columnBuilder;
  private Table table;

  @BeforeEach
  void setUp() {
    constraintEntityLineFormatter = new ConstraintEntityLineFormatter();
    table = Table.builder().name("test_table").columns(List.of()).foreignKeys(List.of()).build();
    columnBuilder = Column.builder().name("col").dataType("varchar").isNullable(false);
  }

  @Nested
  class FormatTests {

    @Test
    void whenNoConstraintsReturnsCurrentAsIs() {
      final var column = columnBuilder.constraints(List.of()).build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value", result);
    }

    @Test
    void whenSingleConstraintAppendsInBrackets() {
      final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value <<UNIQUE>>", result);
    }

    @Test
    void whenMultipleConstraintsAppendsCommaSeparatedInPriorityOrder() {
      final var column =
          columnBuilder.constraints(List.of(Constraint.UNIQUE, Constraint.FK)).build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value <<FK,UNIQUE>>", result);
    }

    @Test
    void whenCurrentIsNullThrowsValidationException() {
      final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> constraintEntityLineFormatter.format(table, column, null));
      assertEquals("current must not be null", exception.getMessage());
    }

    @Test
    void whenCurrentIsEmptyStringAndHasConstraintsAppendsConstraints() {
      final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();
      final var result = constraintEntityLineFormatter.format(table, column, "");
      assertEquals(" <<UNIQUE>>", result);
    }

    @Test
    void whenCurrentIsWhitespaceAndHasConstraintsAppendsConstraints() {
      final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();
      final var result = constraintEntityLineFormatter.format(table, column, "  ");
      assertEquals("   <<UNIQUE>>", result);
    }

    @Test
    void whenColumnIsNullThrowsNullPointerException() {
      assertThrows(
          NullPointerException.class,
          () -> constraintEntityLineFormatter.format(table, null, "value"));
    }

    @Test
    void whenTableIsNullDoesNotThrow() {
      final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();
      assertDoesNotThrow(() -> constraintEntityLineFormatter.format(null, column, "value"));
    }
  }
}
