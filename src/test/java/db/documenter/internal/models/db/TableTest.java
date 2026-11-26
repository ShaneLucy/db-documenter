package db.documenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TableTest {

  @Nested
  class BuilderTests {

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void itThrowsValidationExceptionWhenRequiredFieldIsMissing(
        final String expectedMessage, final Supplier<Table.Builder> incompleteBuilder) {
      final var builder = incompleteBuilder.get();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> incompleteBuilders() {
      return Stream.of(
          Arguments.of(
              "name must not be blank",
              (Supplier<Table.Builder>)
                  () -> Table.builder().columns(List.of()).foreignKeys(List.of())),
          Arguments.of(
              "columns must not be null",
              (Supplier<Table.Builder>) () -> Table.builder().name("t").foreignKeys(List.of())),
          Arguments.of(
              "foreignKeys must not be null",
              (Supplier<Table.Builder>) () -> Table.builder().name("t").columns(List.of())));
    }

    @Test
    void itBuildsTableWithEmptyPrimaryKeyWhenNoneSupplied() {
      final var table = Table.builder().name("t").columns(List.of()).foreignKeys(List.of()).build();

      assertTrue(table.primaryKey().isEmpty());
    }

    @Test
    void itBuildsTableWithSuppliedPrimaryKey() {
      final var pk = PrimaryKey.builder().constraintName("pk").columnNames(List.of("id")).build();

      final var table =
          Table.builder()
              .name("t")
              .columns(List.of())
              .primaryKey(pk)
              .foreignKeys(List.of())
              .build();

      assertEquals(pk, table.primaryKey().orElseThrow());
    }

    @Test
    void itBuildsTableWithPrimaryKeyFromOptional() {
      final var pk = PrimaryKey.builder().constraintName("pk").columnNames(List.of("id")).build();

      final var table =
          Table.builder()
              .name("t")
              .columns(List.of())
              .primaryKey(Optional.of(pk))
              .foreignKeys(List.of())
              .build();

      assertEquals(pk, table.primaryKey().orElseThrow());
    }

    @Test
    void itBuildsTableWithEmptyPrimaryKeyFromEmptyOptional() {
      final var table =
          Table.builder()
              .name("t")
              .columns(List.of())
              .primaryKey(Optional.empty())
              .foreignKeys(List.of())
              .build();

      assertTrue(table.primaryKey().isEmpty());
    }

    @Test
    void itThrowsValidationExceptionWhenColumnsContainNull() {
      final var columns = new ArrayList<Column>();
      columns.add(null);
      final var builder = Table.builder().name("t").columns(columns).foreignKeys(List.of());

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("columns must not contain null elements", exception.getMessage());
    }
  }
}
