package db.documenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import db.documenter.internal.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ColumnTest {

  private Column.Builder columnBuilder;

  @BeforeEach
  void setUp() {
    columnBuilder =
        Column.builder().name("user_id").dataType("varchar").maximumLength(36).isNullable(true);
  }

  @Nested
  class WithConstraintsTests {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void itReplacesConstraintsAndPreservesOtherProperties(final boolean isNullable) {
      final var column =
          columnBuilder.isNullable(isNullable).constraints(List.of(Constraint.UNIQUE)).build();

      final var result = column.withConstraints(List.of(Constraint.FK));

      assertEquals("user_id", result.name());
      assertEquals("varchar", result.dataType());
      assertEquals(36, result.maximumLength());
      assertEquals(isNullable, result.isNullable());
      assertEquals(List.of(Constraint.FK), result.constraints());
    }

    @Test
    void itDefensivelyCopiesConstraints() {
      final var column = columnBuilder.build();
      final var newConstraints = new ArrayList<>(List.of(Constraint.FK));

      final var result = column.withConstraints(newConstraints);
      newConstraints.add(Constraint.UNIQUE);

      assertEquals(List.of(Constraint.FK), result.constraints());
    }

    @Test
    void itDoesNotModifyOriginalColumn() {
      final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();

      column.withConstraints(List.of(Constraint.FK));

      assertEquals(List.of(Constraint.UNIQUE), column.constraints());
    }
  }

  @Nested
  class WithDataTypeTests {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void itPreservesNullabilityAndOtherProperties(final boolean isNullable) {
      final var column =
          columnBuilder.isNullable(isNullable).constraints(List.of(Constraint.UNIQUE)).build();

      final var result = column.withDataType("uuid");

      assertEquals("uuid", result.dataType());
      assertEquals(isNullable, result.isNullable());
      assertEquals("user_id", result.name());
      assertEquals(36, result.maximumLength());
      assertEquals(List.of(Constraint.UNIQUE), result.constraints());
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itThrowsExceptionWhenIsNullableNotSet() {
      final var builder = Column.builder().name("id").dataType("uuid");

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("isNullable must not be null", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void itThrowsValidationExceptionWhenRequiredFieldIsMissing(
        final String expectedMessage, final Supplier<Column.Builder> incompleteBuilder) {
      final var builder = incompleteBuilder.get();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> incompleteBuilders() {
      return Stream.of(
          Arguments.of(
              "name must not be blank",
              (Supplier<Column.Builder>) () -> Column.builder().dataType("uuid").isNullable(false)),
          Arguments.of(
              "dataType must not be blank",
              (Supplier<Column.Builder>) () -> Column.builder().name("id").isNullable(false)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void itBuildsColumnWithExplicitNullability(final boolean isNullable) {
      final var column = columnBuilder.isNullable(isNullable).build();

      assertEquals(isNullable, column.isNullable());
    }
  }
}
