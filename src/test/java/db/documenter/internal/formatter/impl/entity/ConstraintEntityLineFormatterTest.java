package db.documenter.internal.formatter.impl.entity;

import static org.junit.jupiter.api.Assertions.*;

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
    columnBuilder = Column.builder().name("col").dataType("varchar");
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
    void whenMultipleConstraintsAppendsCommaSeparated() {
      final var column =
          columnBuilder
              .constraints(List.of(Constraint.UNIQUE, Constraint.CHECK, Constraint.DEFAULT))
              .build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value <<UNIQUE,DEFAULT,CHECK>>", result);
    }

    @Test
    void whenAllConstraintsAppendsAll() {
      final var column =
          columnBuilder
              .constraints(
                  List.of(
                      Constraint.UNIQUE,
                      Constraint.CHECK,
                      Constraint.DEFAULT,
                      Constraint.AUTO_INCREMENT))
              .build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value <<UNIQUE,AUTO_INCREMENT,DEFAULT,CHECK>>", result);
    }

    @Test
    void whenNullableConstraintAppendsNullable() {
      final var column = columnBuilder.constraints(List.of(Constraint.NULLABLE)).build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value <<NULLABLE>>", result);
    }

    @Test
    void whenMultipleConstraintsIncludingNullableAppendsAllInCorrectOrder() {
      final var column =
          columnBuilder
              .constraints(List.of(Constraint.UNIQUE, Constraint.NULLABLE, Constraint.DEFAULT))
              .build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value <<UNIQUE,DEFAULT,NULLABLE>>", result);
    }

    @Test
    void whenAllConstraintsAppendsAllInCorrectOrder() {
      final var column =
          columnBuilder
              .constraints(
                  List.of(
                      Constraint.UNIQUE,
                      Constraint.NULLABLE,
                      Constraint.DEFAULT,
                      Constraint.FK,
                      Constraint.CHECK,
                      Constraint.AUTO_INCREMENT))
              .build();
      final var result = constraintEntityLineFormatter.format(table, column, "value");
      assertEquals("value <<FK,UNIQUE,AUTO_INCREMENT,DEFAULT,CHECK,NULLABLE>>", result);
    }

    @Test
    void whenCurrentIsNullAndHasConstraintsAppendsConstraints() {
      final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();
      final var result = constraintEntityLineFormatter.format(table, column, null);
      assertEquals("null <<UNIQUE>>", result);
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
