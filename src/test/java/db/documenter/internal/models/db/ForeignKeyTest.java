package db.documenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import db.documenter.internal.exceptions.ValidationException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ForeignKeyTest {

  @Nested
  class BuilderTests {

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void itThrowsValidationExceptionWhenRequiredFieldIsMissing(
        final String expectedMessage, final Supplier<ForeignKey.Builder> incompleteBuilder) {
      final var builder = incompleteBuilder.get();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> incompleteBuilders() {
      return Stream.of(
          Arguments.of(
              "name must not be blank",
              (Supplier<ForeignKey.Builder>)
                  () ->
                      ForeignKey.builder()
                          .sourceTable("orders")
                          .sourceColumn("user_id")
                          .targetTable("users")
                          .targetColumn("id")
                          .referencedSchema("public")),
          Arguments.of(
              "sourceTable must not be blank",
              (Supplier<ForeignKey.Builder>)
                  () ->
                      ForeignKey.builder()
                          .name("fk")
                          .sourceColumn("user_id")
                          .targetTable("users")
                          .targetColumn("id")
                          .referencedSchema("public")),
          Arguments.of(
              "sourceColumn must not be blank",
              (Supplier<ForeignKey.Builder>)
                  () ->
                      ForeignKey.builder()
                          .name("fk")
                          .sourceTable("orders")
                          .targetTable("users")
                          .targetColumn("id")
                          .referencedSchema("public")),
          Arguments.of(
              "targetTable must not be blank",
              (Supplier<ForeignKey.Builder>)
                  () ->
                      ForeignKey.builder()
                          .name("fk")
                          .sourceTable("orders")
                          .sourceColumn("user_id")
                          .targetColumn("id")
                          .referencedSchema("public")),
          Arguments.of(
              "targetColumn must not be blank",
              (Supplier<ForeignKey.Builder>)
                  () ->
                      ForeignKey.builder()
                          .name("fk")
                          .sourceTable("orders")
                          .sourceColumn("user_id")
                          .targetTable("users")
                          .referencedSchema("public")),
          Arguments.of(
              "referencedSchema must not be blank",
              (Supplier<ForeignKey.Builder>)
                  () ->
                      ForeignKey.builder()
                          .name("fk")
                          .sourceTable("orders")
                          .sourceColumn("user_id")
                          .targetTable("users")
                          .targetColumn("id")));
    }
  }
}
