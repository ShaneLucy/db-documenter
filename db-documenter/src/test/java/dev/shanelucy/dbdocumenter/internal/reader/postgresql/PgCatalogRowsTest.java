package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ColumnRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ConstraintRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.EnumRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TableRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TypeRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.UniqueIndexRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PgCatalogRowsTest {

  private static final TypeRow INTEGER = new TypeRow("integer", "pg_catalog", "int4", null, null);

  private static <T> List<T> listWithNull(final T element) {
    return Arrays.asList(element, null);
  }

  @Nested
  class BuilderTests {

    @Test
    void itDefaultsEveryListToEmpty() {
      final var rows = PgCatalogRows.builder().build();

      assertEquals(
          new PgCatalogRows(
              List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
              List.of()),
          rows);
    }

    @Test
    void itBuildsRowsFromEverySetter() {
      final var table = new TableRow(1L, "core", "customers", 1);
      final var column = new ColumnRow(1L, "id", false, INTEGER);
      final var constraint =
          ConstraintRow.builder()
              .name("customers_pkey")
              .type(ConstraintType.PRIMARY_KEY)
              .columns(List.of("id"))
              .build();
      final var index = new UniqueIndexRow(1L, "customers_email_idx", List.of("id"));
      final var enumRow = new EnumRow("core", "Mood", List.of("happy"));

      final var rows =
          PgCatalogRows.builder()
              .requestedSchemas(List.of("core", "secret", "nope"))
              .schemaNames(List.of("core"))
              .schemasWithoutUsage(List.of("secret"))
              .tables(List.of(table))
              .columns(List.of(column))
              .constraints(List.of(constraint))
              .uniqueIndexes(List.of(index))
              .enums(List.of(enumRow))
              .build();

      assertEquals(
          new PgCatalogRows(
              List.of("core", "secret", "nope"),
              List.of("core"),
              List.of("secret"),
              List.of(table),
              List.of(column),
              List.of(constraint),
              List.of(index),
              List.of(enumRow)),
          rows);
    }

    @Test
    void itCopiesListsDefensively() {
      final var schemaNames = new ArrayList<>(List.of("core"));
      final var rows = PgCatalogRows.builder().schemaNames(schemaNames).build();

      schemaNames.add("shop_east");

      assertEquals(List.of("core"), rows.schemaNames());
      assertThrows(UnsupportedOperationException.class, () -> rows.schemaNames().add("x"));
    }

    static Stream<Arguments> nullElementLists() {
      return Stream.of(
          Arguments.of(
              "requestedSchemas",
              (Executable)
                  () -> PgCatalogRows.builder().requestedSchemas(listWithNull("core")).build()),
          Arguments.of(
              "schemasWithoutUsage",
              (Executable)
                  () -> PgCatalogRows.builder().schemasWithoutUsage(listWithNull("core")).build()),
          Arguments.of(
              "schemaNames",
              (Executable) () -> PgCatalogRows.builder().schemaNames(listWithNull("core")).build()),
          Arguments.of(
              "tables",
              (Executable)
                  () ->
                      PgCatalogRows.builder()
                          .tables(listWithNull(new TableRow(1L, "core", "t", 1)))
                          .build()),
          Arguments.of(
              "columns",
              (Executable)
                  () ->
                      PgCatalogRows.builder()
                          .columns(listWithNull(new ColumnRow(1L, "c", true, INTEGER)))
                          .build()),
          Arguments.of(
              "constraints",
              (Executable)
                  () ->
                      PgCatalogRows.builder()
                          .constraints(
                              listWithNull(
                                  ConstraintRow.builder()
                                      .name("k")
                                      .type(ConstraintType.PRIMARY_KEY)
                                      .columns(List.of("id"))
                                      .build()))
                          .build()),
          Arguments.of(
              "uniqueIndexes",
              (Executable)
                  () ->
                      PgCatalogRows.builder()
                          .uniqueIndexes(listWithNull(new UniqueIndexRow(1L, "i", List.of("id"))))
                          .build()),
          Arguments.of(
              "enums",
              (Executable)
                  () ->
                      PgCatalogRows.builder()
                          .enums(listWithNull(new EnumRow("core", "Mood", List.of())))
                          .build()));
    }

    @ParameterizedTest
    @MethodSource("nullElementLists")
    void itThrowsExceptionWhenListContainsNull(final String property, final Executable build) {
      final var exception = assertThrows(ValidationException.class, build);

      assertEquals(property + " must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenListIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class, () -> PgCatalogRows.builder().tables(null).build());

      assertEquals("tables must not be null", exception.getMessage());
    }
  }

  @Nested
  class ConstraintRowTests {

    @Test
    void itDefaultsReferenceToNoneAndActionsToNoAction() {
      final var row =
          ConstraintRow.builder()
              .name("customers_pkey")
              .type(ConstraintType.PRIMARY_KEY)
              .columns(List.of("id"))
              .build();

      assertEquals(0L, row.tableOid());
      assertNull(row.referencedTable());
      assertEquals(List.of(), row.referencedColumns());
      assertEquals('a', row.onDelete());
      assertEquals('a', row.onUpdate());
    }

    @Test
    void itBuildsForeignKeyFromEverySetter() {
      final var target = QualifiedName.of("core", "customers");

      final var row =
          ConstraintRow.builder()
              .tableOid(7L)
              .name("orders_uid_fkey")
              .type(ConstraintType.FOREIGN_KEY)
              .columns(List.of("uid"))
              .referencedTable(target)
              .referencedColumns(List.of("id"))
              .onDelete('c')
              .onUpdate('n')
              .build();

      assertEquals(
          new ConstraintRow(
              7L,
              "orders_uid_fkey",
              ConstraintType.FOREIGN_KEY,
              List.of("uid"),
              target,
              List.of("id"),
              'c',
              'n'),
          row);
    }

    @Test
    void itThrowsExceptionWhenNameIsNotSet() {
      final var builder =
          ConstraintRow.builder().type(ConstraintType.PRIMARY_KEY).columns(List.of("id"));

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsEmpty() {
      final var builder =
          ConstraintRow.builder().name("").type(ConstraintType.PRIMARY_KEY).columns(List.of("id"));

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTypeIsNotSet() {
      final var builder = ConstraintRow.builder().name("k").columns(List.of("id"));

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("type must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenColumnsAreNotSet() {
      final var builder = ConstraintRow.builder().name("k").type(ConstraintType.PRIMARY_KEY);

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("columns must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenColumnsContainNull() {
      final var builder =
          ConstraintRow.builder()
              .name("k")
              .type(ConstraintType.PRIMARY_KEY)
              .columns(listWithNull("id"));

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("columns must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenReferencedColumnsContainNull() {
      final var builder =
          ConstraintRow.builder()
              .name("k")
              .type(ConstraintType.FOREIGN_KEY)
              .columns(List.of("id"))
              .referencedColumns(listWithNull("id"));

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("referencedColumns must not contain null elements", exception.getMessage());
    }

    @Test
    void itCopiesColumnsDefensively() {
      final var columns = new ArrayList<>(List.of("a"));
      final var row =
          ConstraintRow.builder().name("k").type(ConstraintType.UNIQUE).columns(columns).build();

      columns.add("b");

      assertEquals(List.of("a"), row.columns());
    }
  }

  @Nested
  class RowValidationTests {

    @Test
    void itThrowsExceptionWhenTableRowSchemaIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> new TableRow(1L, null, "t", 1));

      assertEquals("schema must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTableRowSchemaIsEmpty() {
      final var exception =
          assertThrows(ValidationException.class, () -> new TableRow(1L, "", "t", 1));

      assertEquals("schema must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTableRowColumnCountIsNegative() {
      final var exception =
          assertThrows(ValidationException.class, () -> new TableRow(1L, "core", "t", -1));

      assertEquals("columnCount must not be negative, but is -1", exception.getMessage());
    }

    @Test
    void itAcceptsTableRowWithZeroColumns() {
      assertEquals(0, new TableRow(1L, "core", "t", 0).columnCount());
    }

    @Test
    void itThrowsExceptionWhenTableRowNameIsEmpty() {
      final var exception =
          assertThrows(ValidationException.class, () -> new TableRow(1L, "core", "", 1));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenColumnRowNameIsEmpty() {
      final var exception =
          assertThrows(ValidationException.class, () -> new ColumnRow(1L, "", true, INTEGER));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenColumnRowTypeIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> new ColumnRow(1L, "c", true, null));

      assertEquals("type must not be null", exception.getMessage());
    }

    @Test
    void itAcceptsEmptyFormattedNameInTypeRow() {
      final var type = new TypeRow("", "core", "t", null, null);

      assertEquals("", type.formattedName());
    }

    @Test
    void itThrowsExceptionWhenTypeRowFormattedNameIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class, () -> new TypeRow(null, "pg_catalog", "int4", null, null));

      assertEquals("formattedName must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTypeRowSchemaIsEmpty() {
      final var exception =
          assertThrows(
              ValidationException.class, () -> new TypeRow("integer", "", "int4", null, null));

      assertEquals("schema must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenTypeRowNameIsEmpty() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new TypeRow("integer", "pg_catalog", "", null, null));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenUniqueIndexRowNameIsEmpty() {
      final var exception =
          assertThrows(ValidationException.class, () -> new UniqueIndexRow(1L, "", List.of("a")));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenUniqueIndexRowColumnsContainNull() {
      final var exception =
          assertThrows(
              ValidationException.class, () -> new UniqueIndexRow(1L, "idx", listWithNull("a")));

      assertEquals("columns must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumRowSchemaIsEmpty() {
      final var exception =
          assertThrows(ValidationException.class, () -> new EnumRow("", "Mood", List.of()));

      assertEquals("schema must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumRowNameIsEmpty() {
      final var exception =
          assertThrows(ValidationException.class, () -> new EnumRow("core", "", List.of()));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumRowLabelsContainNull() {
      final var exception =
          assertThrows(
              ValidationException.class, () -> new EnumRow("core", "Mood", listWithNull("happy")));

      assertEquals("labels must not contain null elements", exception.getMessage());
    }
  }
}
