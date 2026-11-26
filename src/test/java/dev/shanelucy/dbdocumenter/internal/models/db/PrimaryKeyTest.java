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

class PrimaryKeyTest {

  @Nested
  class BuilderTests {

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void itThrowsValidationExceptionWhenRequiredFieldIsMissing(
        final String expectedMessage, final Supplier<PrimaryKey.Builder> incompleteBuilder) {
      final var builder = incompleteBuilder.get();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> incompleteBuilders() {
      return Stream.of(
          Arguments.of(
              "constraintName must not be blank",
              (Supplier<PrimaryKey.Builder>) () -> PrimaryKey.builder().columnNames(List.of("id"))),
          Arguments.of(
              "columnNames must contain at least 1 item",
              (Supplier<PrimaryKey.Builder>) () -> PrimaryKey.builder().constraintName("pk")));
    }
  }
}
