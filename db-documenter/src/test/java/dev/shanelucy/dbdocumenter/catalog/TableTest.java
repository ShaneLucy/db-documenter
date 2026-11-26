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
import org.junit.jupiter.params.provider.MethodSource;

class TableTest {

  private static final QualifiedName ORDERS = QualifiedName.of("app", "orders");
  private static final QualifiedName USERS = QualifiedName.of("app", "users");

  private static Column column(final String name) {
    return Column.builder()
        .name(name)
        .nullable(false)
        .dataType(DataType.builder().displayText("int4").build())
        .build();
  }

  private static UniqueKey uniqueKey(final String name, final String... columns) {
    return UniqueKey.builder().name(name).columns(List.of(columns)).build();
  }

  private static ForeignKey foreignKey(
      final String column, final QualifiedName referencedTable, final String referencedColumn) {
    return ForeignKey.builder()
        .columns(List.of(column))
        .referencedTable(referencedTable)
        .referencedColumns(List.of(referencedColumn))
        .build();
  }

  private static Table.Builder ordersTable() {
    return Table.builder()
        .name(ORDERS)
        .columns(List.of(column("id"), column("user_id"), column("code"), column("tenant_id")));
  }

  @Nested
  class BuilderTests {

    @Test
    void itDefaultsListsToEmptyAndPrimaryKeyToAbsent() {
      final var table = Table.builder().name(ORDERS).build();

      assertEquals(ORDERS, table.name());
      assertTrue(table.columns().isEmpty());
      assertTrue(table.primaryKey().isEmpty());
      assertTrue(table.uniqueKeys().isEmpty());
      assertTrue(table.foreignKeys().isEmpty());
    }

    @Test
    void itBuildsTableWithAllComponents() {
      final var primaryKey = PrimaryKey.builder().columns(List.of("id")).build();
      final var unique = uniqueKey("orders_code_key", "code");
      final var foreignKey = foreignKey("user_id", USERS, "id");

      final var table =
          ordersTable()
              .primaryKey(primaryKey)
              .uniqueKeys(List.of(unique))
              .foreignKeys(List.of(foreignKey))
              .build();

      assertEquals(Optional.of(primaryKey), table.primaryKey());
      assertEquals(List.of(unique), table.uniqueKeys());
      assertEquals(List.of(foreignKey), table.foreignKeys());
    }

    @Test
    void itTreatsNullPrimaryKeyAsAbsent() {
      final var table = ordersTable().primaryKey(null).build();

      assertTrue(table.primaryKey().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenNameIsNotSet() {
      final var builder = Table.builder().columns(List.of(column("id")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("name must not be null", exception.getMessage());
    }
  }

  @Nested
  class ValidationTests {

    @Test
    void itThrowsExceptionWhenNameIsNull() {
      final List<Column> columns = List.of();
      final List<UniqueKey> uniqueKeys = List.of();
      final List<ForeignKey> foreignKeys = List.of();

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Table(null, columns, Optional.empty(), uniqueKeys, foreignKeys));

      assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenPrimaryKeyOptionalIsNull() {
      final List<Column> columns = List.of();
      final List<UniqueKey> uniqueKeys = List.of();
      final List<ForeignKey> foreignKeys = List.of();

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Table(ORDERS, columns, null, uniqueKeys, foreignKeys));

      assertEquals("primaryKey must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenColumnsAreNull() {
      final var builder = Table.builder().name(ORDERS).columns(null);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("columns must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenColumnsContainNull() {
      final var builder = Table.builder().name(ORDERS).columns(Arrays.asList(column("id"), null));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("columns must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenUniqueKeysAreNull() {
      final var builder = ordersTable().uniqueKeys(null);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("uniqueKeys must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenUniqueKeysContainNull() {
      final var builder = ordersTable().uniqueKeys(Arrays.asList((UniqueKey) null));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("uniqueKeys must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenForeignKeysAreNull() {
      final var builder = ordersTable().foreignKeys(null);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("foreignKeys must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenForeignKeysContainNull() {
      final var builder = ordersTable().foreignKeys(Arrays.asList((ForeignKey) null));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("foreignKeys must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionNamingDuplicateColumn() {
      final var builder =
          Table.builder().name(ORDERS).columns(List.of(column("id"), column("code"), column("id")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "columns.name must not contain duplicates, found duplicate: id", exception.getMessage());
    }

    @Test
    void itTreatsColumnNamesAsCaseSensitive() {
      final var table = Table.builder().name(ORDERS).columns(List.of(column("id"), column("ID")));

      assertEquals(2, table.build().columns().size());
    }

    @Test
    void itThrowsExceptionWhenPrimaryKeyColumnIsNotInTable() {
      final var builder =
          ordersTable().primaryKey(PrimaryKey.builder().columns(List.of("id", "missing")).build());

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "primaryKey refers to column missing, which is not in the table", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenUniqueKeyColumnIsNotInTable() {
      final var builder =
          ordersTable().uniqueKeys(List.of(uniqueKey("uq", "code"), uniqueKey("uq2", "Code")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "uniqueKeys refers to column Code, which is not in the table", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenForeignKeyLocalColumnIsNotInTable() {
      final var builder =
          ordersTable().foreignKeys(List.of(foreignKey("customer_id", USERS, "id")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "foreignKeys refers to column customer_id, which is not in the table",
          exception.getMessage());
    }

    @Test
    void itAcceptsForeignKeyToTableOutsideTheSnapshot() {
      final var dangling = foreignKey("user_id", QualifiedName.of("elsewhere", "ghosts"), "nope");

      final var table = ordersTable().foreignKeys(List.of(dangling)).build();

      assertEquals(List.of(dangling), table.foreignKeys());
    }

    @Test
    void itAcceptsEmptyColumnsTable() {
      final var table = Table.builder().name(ORDERS).columns(List.of()).build();

      assertTrue(table.columns().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenEmptyColumnsTableHasPrimaryKey() {
      final var builder =
          Table.builder()
              .name(ORDERS)
              .primaryKey(PrimaryKey.builder().columns(List.of("id")).build());

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "primaryKey refers to column id, which is not in the table", exception.getMessage());
    }
  }

  @Nested
  class OrderingTests {

    static Stream<Arguments> columnOrders() {
      return Stream.of(
          Arguments.of(List.of("id", "user_id", "code")),
          Arguments.of(List.of("code", "user_id", "id")),
          Arguments.of(List.of("user_id", "id", "code")));
    }

    @ParameterizedTest
    @MethodSource("columnOrders")
    void itKeepsColumnsInGivenOrder(final List<String> names) {
      final var columns = names.stream().map(TableTest::column).toList();

      final var table = Table.builder().name(ORDERS).columns(columns).build();

      assertEquals(names, table.columns().stream().map(Column::name).toList());
    }

    @Test
    void itSortsUniqueKeysCanonically() {
      final var byTenantAndCode = uniqueKey("b", "tenant_id", "code");
      final var byCode = uniqueKey("z", "code");
      final var byCodeAndTenant = uniqueKey("a", "code", "tenant_id");
      final var byCodeUnnamed = UniqueKey.builder().columns(List.of("code")).build();

      final var table =
          ordersTable()
              .uniqueKeys(List.of(byTenantAndCode, byCode, byCodeAndTenant, byCodeUnnamed))
              .build();

      assertEquals(
          List.of(byCodeUnnamed, byCode, byCodeAndTenant, byTenantAndCode), table.uniqueKeys());
    }

    @Test
    void itSortsForeignKeysCanonically() {
      final var toUsersByCode = foreignKey("code", USERS, "code");
      final var toAccounts = foreignKey("user_id", QualifiedName.of("app", "accounts"), "id");
      final var toUsers = foreignKey("user_id", USERS, "id");

      final var table =
          ordersTable().foreignKeys(List.of(toUsers, toAccounts, toUsersByCode)).build();

      assertEquals(List.of(toUsersByCode, toAccounts, toUsers), table.foreignKeys());
    }

    @Test
    void itDoesNotReorderKeyColumns() {
      final var primaryKey = PrimaryKey.builder().columns(List.of("tenant_id", "id")).build();
      final var unique = uniqueKey("uq", "tenant_id", "code");
      final var foreignKey =
          ForeignKey.builder()
              .columns(List.of("user_id", "tenant_id"))
              .referencedTable(USERS)
              .referencedColumns(List.of("id", "tenant_id"))
              .build();

      final var table =
          ordersTable()
              .primaryKey(primaryKey)
              .uniqueKeys(List.of(unique))
              .foreignKeys(List.of(foreignKey))
              .build();

      assertEquals(List.of("tenant_id", "id"), table.primaryKey().orElseThrow().columns());
      assertEquals(List.of("tenant_id", "code"), table.uniqueKeys().getFirst().columns());
      assertEquals(List.of("user_id", "tenant_id"), table.foreignKeys().getFirst().columns());
      assertEquals(List.of("id", "tenant_id"), table.foreignKeys().getFirst().referencedColumns());
    }

    @Test
    void itProducesEqualTablesWhateverTheKeyInputOrder() {
      final var first = uniqueKey("a", "code");
      final var second = uniqueKey("b", "tenant_id");
      final var fk1 = foreignKey("user_id", USERS, "id");
      final var fk2 = foreignKey("tenant_id", USERS, "tenant_id");

      final var forwards =
          ordersTable().uniqueKeys(List.of(first, second)).foreignKeys(List.of(fk1, fk2)).build();
      final var backwards =
          ordersTable().uniqueKeys(List.of(second, first)).foreignKeys(List.of(fk2, fk1)).build();

      assertEquals(forwards, backwards);
    }
  }

  @Nested
  class DefensiveCopyTests {

    @Test
    void itDefensivelyCopiesAllLists() {
      final var columns = new ArrayList<>(List.of(column("id"), column("code")));
      final var uniqueKeys = new ArrayList<>(List.of(uniqueKey("uq", "code")));
      final var foreignKeys = new ArrayList<>(List.of(foreignKey("id", USERS, "id")));
      final var table =
          Table.builder()
              .name(ORDERS)
              .columns(columns)
              .uniqueKeys(uniqueKeys)
              .foreignKeys(foreignKeys)
              .build();

      columns.clear();
      uniqueKeys.clear();
      foreignKeys.clear();

      assertEquals(2, table.columns().size());
      assertEquals(1, table.uniqueKeys().size());
      assertEquals(1, table.foreignKeys().size());
    }

    @Test
    void itReturnsUnmodifiableLists() {
      final var table =
          ordersTable()
              .uniqueKeys(new ArrayList<>(List.of(uniqueKey("uq", "code"))))
              .foreignKeys(new ArrayList<>(List.of(foreignKey("user_id", USERS, "id"))))
              .build();
      final var columns = table.columns();
      final var uniqueKeys = table.uniqueKeys();
      final var foreignKeys = table.foreignKeys();

      assertThrows(UnsupportedOperationException.class, () -> columns.add(column("x")));
      assertThrows(UnsupportedOperationException.class, uniqueKeys::clear);
      assertThrows(UnsupportedOperationException.class, foreignKeys::clear);
    }
  }
}
