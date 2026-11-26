package dev.shanelucy.dbdocumenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DbEnumTest {

  @Nested
  class BuilderTests {

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void itThrowsValidationExceptionWhenRequiredFieldIsMissing(
        final String expectedMessage, final Supplier<DbEnum.Builder> incompleteBuilder) {
      final var builder = incompleteBuilder.get();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> incompleteBuilders() {
      return Stream.of(
          Arguments.of(
              "schemaName must not be blank",
              (Supplier<DbEnum.Builder>)
                  () -> DbEnum.builder().enumName("status").enumValues(List.of("A"))),
          Arguments.of(
              "enumName must not be blank",
              (Supplier<DbEnum.Builder>)
                  () -> DbEnum.builder().schemaName("public").enumValues(List.of("A"))),
          Arguments.of(
              "enumValues must not be null",
              (Supplier<DbEnum.Builder>)
                  () -> DbEnum.builder().schemaName("public").enumName("status")));
    }
  }
}
