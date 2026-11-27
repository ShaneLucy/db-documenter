package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SchemaTest {

  private static Table table(final String schema, final String name) {
    return Table.builder().name(QualifiedName.of(schema, name)).build();
  }

  private static EnumType enumType(final String schema, final String name) {
    return EnumType.builder().name(QualifiedName.of(schema, name)).build();
  }

  @Nested
  class BuilderTests {

    @Test
    void itDefaultsListsToEmpty() {
      final var schema = Schema.builder().name("app").build();

      assertEquals("app", schema.name());
      assertTrue(schema.tables().isEmpty());
      assertTrue(schema.enumTypes().isEmpty());
    }

    @Test
    void itBuildsSchemaWithTablesAndEnumTypes() {
      final var users = table("app", "users");
      final var mood = enumType("app", "mood");

      final var schema =
          Schema.builder().name("app").tables(List.of(users)).enumTypes(List.of(mood)).build();

      assertEquals(new Schema("app", List.of(users), List.of(mood)), schema);
    }

    @Test
    void itThrowsExceptionWhenNameIsNotSet() {
      final var builder = Schema.builder();

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be null", exception.getMessage());
    }
  }

  @Nested
  class ValidationTests {

    @ParameterizedTest
    @ValueSource(strings = {" ", "My Schema"})
    void itAcceptsWhitespaceAndMixedCaseNames(final String name) {
      final var schema = Schema.builder().name(name).tables(List.of(table(name, "t"))).build();

      assertEquals(name, schema.name());
    }

    @Test
    void itThrowsExceptionWhenNameIsEmpty() {
      final var builder = Schema.builder().name("");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTablesAreNull() {
      final var builder = Schema.builder().name("app").tables(null);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("tables must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTablesContainNull() {
      final var builder = Schema.builder().name("app").tables(Arrays.asList((Table) null));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("tables must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumTypesAreNull() {
      final var builder = Schema.builder().name("app").enumTypes(null);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("enumTypes must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumTypesContainNull() {
      final var builder = Schema.builder().name("app").enumTypes(Arrays.asList((EnumType) null));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("enumTypes must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTableBelongsToAnotherSchema() {
      final var builder =
          Schema.builder().name("app").tables(List.of(table("app", "a"), table("App", "b")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "tables must all belong to schema app, but contains \"App\".b", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumTypeBelongsToAnotherSchema() {
      final var builder = Schema.builder().name("app").enumTypes(List.of(enumType("auth", "mood")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "enumTypes must all belong to schema app, but contains auth.mood",
          exception.getMessage());
    }

    @Test
    void itThrowsExceptionNamingDuplicateTable() {
      final var builder =
          Schema.builder()
              .name("app")
              .tables(
                  List.of(table("app", "users"), table("app", "orders"), table("app", "users")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "tables.name must not contain duplicates, found duplicate: " + "app.users",
          exception.getMessage());
    }

    @Test
    void itThrowsExceptionNamingDuplicateEnumType() {
      final var builder =
          Schema.builder()
              .name("app")
              .enumTypes(List.of(enumType("app", "mood"), enumType("app", "mood")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "enumTypes.name must not contain duplicates, found duplicate: " + "app.mood",
          exception.getMessage());
    }

    @Test
    void itAcceptsTableAndEnumTypeWithTheSameName() {
      final var schema =
          Schema.builder()
              .name("app")
              .tables(List.of(table("app", "status")))
              .enumTypes(List.of(enumType("app", "status")))
              .build();

      assertEquals(1, schema.tables().size());
      assertEquals(1, schema.enumTypes().size());
    }
  }

  @Nested
  class OrderingTests {

    @Test
    void itSortsTablesByNameInStringOrder() {
      final var schema =
          Schema.builder()
              .name("app")
              .tables(
                  List.of(
                      table("app", "users"),
                      table("app", "Orders"),
                      table("app", "orders"),
                      table("app", "accounts")))
              .build();

      assertEquals(
          List.of("Orders", "accounts", "orders", "users"),
          schema.tables().stream().map(table -> table.name().name()).toList());
    }

    @Test
    void itSortsEnumTypesByName() {
      final var schema =
          Schema.builder()
              .name("app")
              .enumTypes(List.of(enumType("app", "status"), enumType("app", "mood")))
              .build();

      assertEquals(
          List.of("mood", "status"),
          schema.enumTypes().stream().map(enumType -> enumType.name().name()).toList());
    }

    @Test
    void itDoesNotReorderEnumValues() {
      final var mood =
          EnumType.builder()
              .name(QualifiedName.of("app", "mood"))
              .values(List.of("sad", "ok", "happy"))
              .build();

      final var schema = Schema.builder().name("app").enumTypes(List.of(mood)).build();

      assertEquals(List.of("sad", "ok", "happy"), schema.enumTypes().getFirst().values());
    }
  }

  @Nested
  class DefensiveCopyTests {

    @Test
    void itDefensivelyCopiesBothLists() {
      final var tables = new ArrayList<>(List.of(table("app", "users")));
      final var enumTypes = new ArrayList<>(List.of(enumType("app", "mood")));
      final var schema = new Schema("app", tables, enumTypes);

      tables.add(table("app", "orders"));
      enumTypes.clear();

      assertEquals(List.of(table("app", "users")), schema.tables());
      assertEquals(List.of(enumType("app", "mood")), schema.enumTypes());
    }

    @Test
    void itReturnsUnmodifiableLists() {
      final var schema =
          new Schema(
              "app",
              new ArrayList<>(List.of(table("app", "users"))),
              new ArrayList<>(List.of(enumType("app", "mood"))));
      final var tables = schema.tables();
      final var enumTypes = schema.enumTypes();

      assertThrows(UnsupportedOperationException.class, tables::clear);
      assertThrows(UnsupportedOperationException.class, enumTypes::clear);
    }
  }
}
