package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ForeignKeyTest {

  private static final QualifiedName USERS = QualifiedName.of("app", "users");

  private static ForeignKey foreignKey(
      final List<String> columns, final List<String> referencedColumns) {
    return new ForeignKey(
        Optional.empty(),
        columns,
        USERS,
        referencedColumns,
        ReferentialAction.NO_ACTION,
        ReferentialAction.NO_ACTION);
  }

  @Nested
  class ConstructorTests {

    static Stream<Arguments> invalidColumnLists() {
      return Stream.of(
          Arguments.of(null, List.of("id"), "columns must contain at least 1 item"),
          Arguments.of(List.of(), List.of("id"), "columns must contain at least 1 item"),
          Arguments.of(
              Arrays.asList("user_id", null),
              List.of("id", "tenant_id"),
              "columns must not contain null elements"),
          Arguments.of(List.of(""), List.of("id"), "columns must not be empty"),
          Arguments.of(List.of("user_id"), null, "referencedColumns must contain at least 1 item"),
          Arguments.of(
              List.of("user_id"), List.of(), "referencedColumns must contain at least 1 item"),
          Arguments.of(
              List.of("user_id"),
              Arrays.asList((String) null),
              "referencedColumns must not contain null elements"),
          Arguments.of(List.of("user_id"), List.of(""), "referencedColumns must not be empty"),
          Arguments.of(
              List.of("tenant_id", "user_id"),
              List.of("id"),
              "columns and referencedColumns must have the same size, but have 2 and 1"),
          Arguments.of(
              List.of("user_id"),
              List.of("tenant_id", "id"),
              "columns and referencedColumns must have the same size, but have 1 and 2"));
    }

    @Test
    void itKeepsColumnPairsInKeyOrder() {
      final var foreignKey =
          foreignKey(List.of("user_tenant", "user_id"), List.of("tenant_id", "id"));

      assertEquals(List.of("user_tenant", "user_id"), foreignKey.columns());
      assertEquals(List.of("tenant_id", "id"), foreignKey.referencedColumns());
    }

    @Test
    void itAcceptsDuplicateLocalColumnsAndKeepsTheirOrder() {
      final var foreignKey = foreignKey(List.of("a", "a"), List.of("a", "b"));

      assertEquals(List.of("a", "a"), foreignKey.columns());
      assertEquals(List.of("a", "b"), foreignKey.referencedColumns());
    }

    @Test
    void itAcceptsDuplicateReferencedColumnsAndKeepsTheirOrder() {
      final var foreignKey = foreignKey(List.of("a", "b"), List.of("id", "id"));

      assertEquals(List.of("a", "b"), foreignKey.columns());
      assertEquals(List.of("id", "id"), foreignKey.referencedColumns());
    }

    @Test
    void itThrowsExceptionWhenNameOptionalIsNull() {
      final var columns = List.of("user_id");
      final var referencedColumns = List.of("id");

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new ForeignKey(
                      null,
                      columns,
                      USERS,
                      referencedColumns,
                      ReferentialAction.NO_ACTION,
                      ReferentialAction.NO_ACTION));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsEmpty() {
      final var name = Optional.of("");
      final var columns = List.of("user_id");
      final var referencedColumns = List.of("id");

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new ForeignKey(
                      name,
                      columns,
                      USERS,
                      referencedColumns,
                      ReferentialAction.NO_ACTION,
                      ReferentialAction.NO_ACTION));

      assertEquals("name must not be empty", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidColumnLists")
    void itThrowsExceptionWhenColumnListsAreInvalid(
        final List<String> columns,
        final List<String> referencedColumns,
        final String expectedMessage) {
      final var exception =
          assertThrows(
              IllegalArgumentException.class, () -> foreignKey(columns, referencedColumns));

      assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenReferencedTableIsNull() {
      final var columns = List.of("user_id");
      final var referencedColumns = List.of("id");

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new ForeignKey(
                      Optional.empty(),
                      columns,
                      null,
                      referencedColumns,
                      ReferentialAction.NO_ACTION,
                      ReferentialAction.NO_ACTION));

      assertEquals("referencedTable must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenOnDeleteIsNull() {
      final var columns = List.of("user_id");
      final var referencedColumns = List.of("id");

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new ForeignKey(
                      Optional.empty(),
                      columns,
                      USERS,
                      referencedColumns,
                      null,
                      ReferentialAction.NO_ACTION));

      assertEquals("onDelete must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenOnUpdateIsNull() {
      final var columns = List.of("user_id");
      final var referencedColumns = List.of("id");

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new ForeignKey(
                      Optional.empty(),
                      columns,
                      USERS,
                      referencedColumns,
                      ReferentialAction.NO_ACTION,
                      null));

      assertEquals("onUpdate must not be null", exception.getMessage());
    }

    @Test
    void itDefensivelyCopiesBothColumnLists() {
      final var columns = new ArrayList<>(List.of("user_id"));
      final var referencedColumns = new ArrayList<>(List.of("id"));
      final var foreignKey = foreignKey(columns, referencedColumns);

      columns.set(0, "other_id");
      referencedColumns.set(0, "other");

      assertEquals(List.of("user_id"), foreignKey.columns());
      assertEquals(List.of("id"), foreignKey.referencedColumns());
    }

    @Test
    void itReturnsUnmodifiableColumnLists() {
      final var foreignKey =
          foreignKey(new ArrayList<>(List.of("user_id")), new ArrayList<>(List.of("id")));
      final var columns = foreignKey.columns();
      final var referencedColumns = foreignKey.referencedColumns();

      assertThrows(UnsupportedOperationException.class, () -> columns.add("x"));
      assertThrows(UnsupportedOperationException.class, () -> referencedColumns.add("x"));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itDefaultsBothActionsToNoActionAndNameToAbsent() {
      final var foreignKey =
          ForeignKey.builder()
              .columns(List.of("user_id"))
              .referencedTable(USERS)
              .referencedColumns(List.of("id"))
              .build();

      assertEquals(ReferentialAction.NO_ACTION, foreignKey.onDelete());
      assertEquals(ReferentialAction.NO_ACTION, foreignKey.onUpdate());
      assertTrue(foreignKey.name().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(ReferentialAction.class)
    void itBuildsForeignKeyWithEveryAction(final ReferentialAction action) {
      final var foreignKey =
          ForeignKey.builder()
              .name("orders_user_id_fkey")
              .columns(List.of("user_id"))
              .referencedTable(USERS)
              .referencedColumns(List.of("id"))
              .onDelete(action)
              .onUpdate(action)
              .build();

      assertEquals(
          new ForeignKey(
              Optional.of("orders_user_id_fkey"),
              List.of("user_id"),
              USERS,
              List.of("id"),
              action,
              action),
          foreignKey);
    }

    @Test
    void itTreatsNullNameAsAbsent() {
      final var foreignKey =
          ForeignKey.builder()
              .name(null)
              .columns(List.of("user_id"))
              .referencedTable(USERS)
              .referencedColumns(List.of("id"))
              .build();

      assertTrue(foreignKey.name().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenColumnsAreNotSet() {
      final var builder =
          ForeignKey.builder().referencedTable(USERS).referencedColumns(List.of("id"));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("columns must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenReferencedTableIsNotSet() {
      final var builder =
          ForeignKey.builder().columns(List.of("user_id")).referencedColumns(List.of("id"));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("referencedTable must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenReferencedColumnsAreNotSet() {
      final var builder = ForeignKey.builder().columns(List.of("user_id")).referencedTable(USERS);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("referencedColumns must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenBuiltWithMismatchedColumnLists() {
      final var builder =
          ForeignKey.builder()
              .columns(List.of("user_id"))
              .referencedTable(USERS)
              .referencedColumns(List.of("tenant_id", "id"));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "columns and referencedColumns must have the same size, but have 1 and 2",
          exception.getMessage());
    }
  }
}
