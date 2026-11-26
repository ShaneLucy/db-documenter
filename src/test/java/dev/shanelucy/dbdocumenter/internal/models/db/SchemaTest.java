package db.documenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import db.documenter.internal.exceptions.ValidationException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SchemaTest {

  @Nested
  class BuilderTests {

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void itThrowsValidationExceptionWhenRequiredFieldIsMissing(
        final String expectedMessage, final Supplier<Schema.Builder> incompleteBuilder) {
      final var builder = incompleteBuilder.get();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> incompleteBuilders() {
      return Stream.of(
          Arguments.of(
              "name must not be blank",
              (Supplier<Schema.Builder>)
                  () -> Schema.builder().tables(List.of()).dbEnums(List.of())),
          Arguments.of(
              "tables must not be null",
              (Supplier<Schema.Builder>) () -> Schema.builder().name("public").dbEnums(List.of())),
          Arguments.of(
              "dbEnums must not be null",
              (Supplier<Schema.Builder>) () -> Schema.builder().name("public").tables(List.of())));
    }
  }
}
