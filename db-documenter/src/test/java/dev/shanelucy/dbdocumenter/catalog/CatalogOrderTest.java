package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CatalogOrderTest {

  private static final QualifiedName ACCOUNTS = QualifiedName.of("app", "accounts");
  private static final QualifiedName USERS = QualifiedName.of("app", "users");

  private static UniqueKey uniqueKey(final Optional<String> name, final List<String> columns) {
    return new UniqueKey(name, columns);
  }

  private static ForeignKey foreignKey(
      final List<String> columns,
      final QualifiedName referencedTable,
      final List<String> referencedColumns,
      final ReferentialAction onDelete,
      final ReferentialAction onUpdate,
      final Optional<String> name) {
    return new ForeignKey(name, columns, referencedTable, referencedColumns, onDelete, onUpdate);
  }

  private static ForeignKey baseForeignKey(final List<String> columns) {
    return foreignKey(
        columns,
        USERS,
        List.of("id"),
        ReferentialAction.NO_ACTION,
        ReferentialAction.NO_ACTION,
        Optional.of("fk"));
  }

  private static <T> void assertStrictlyBefore(
      final Comparator<T> order, final T smaller, final T larger) {
    assertTrue(order.compare(smaller, larger) < 0, smaller + " should sort before " + larger);
    assertTrue(order.compare(larger, smaller) > 0, larger + " should sort after " + smaller);
  }

  @Nested
  class CompareListsTests {

    static Stream<Arguments> orderedLists() {
      return Stream.of(
          Arguments.of(List.of(), List.of("a")),
          Arguments.of(List.of("a"), List.of("b")),
          Arguments.of(List.of("a"), List.of("a", "b")),
          Arguments.of(List.of("a", "z"), List.of("b")),
          Arguments.of(List.of("a", "b"), List.of("a", "c")),
          Arguments.of(List.of("B"), List.of("a")));
    }

    @ParameterizedTest
    @MethodSource("orderedLists")
    void itOrdersElementByElementWithShorterPrefixFirst(
        final List<String> smaller, final List<String> larger) {
      assertStrictlyBefore(CatalogOrder::compareLists, smaller, larger);
    }

    @Test
    void itComparesEqualListsAsZero() {
      assertEquals(0, CatalogOrder.compareLists(List.of("a", "b"), List.of("a", "b")));
    }

    @Test
    void itComparesEmptyListsAsZero() {
      assertEquals(0, CatalogOrder.compareLists(List.<String>of(), List.<String>of()));
    }
  }

  @Nested
  class CompareOptionalNamesTests {

    @Test
    void itComparesTwoAbsentNamesAsZero() {
      assertEquals(0, CatalogOrder.compareOptionalNames(Optional.empty(), Optional.empty()));
    }

    @Test
    void itSortsAbsentNameBeforePresentName() {
      assertStrictlyBefore(CatalogOrder::compareOptionalNames, Optional.empty(), Optional.of("a"));
    }

    @Test
    void itSortsPresentNamesInStringOrder() {
      assertStrictlyBefore(CatalogOrder::compareOptionalNames, Optional.of("A"), Optional.of("a"));
    }

    @Test
    void itComparesEqualPresentNamesAsZero() {
      assertEquals(0, CatalogOrder.compareOptionalNames(Optional.of("a"), Optional.of("a")));
    }
  }

  @Nested
  class UniqueKeysTests {

    static Stream<Arguments> orderedUniqueKeys() {
      return Stream.of(
          Arguments.of(
              uniqueKey(Optional.of("z"), List.of("a")), uniqueKey(Optional.of("a"), List.of("b"))),
          Arguments.of(
              uniqueKey(Optional.of("z"), List.of("a")),
              uniqueKey(Optional.of("a"), List.of("a", "b"))),
          Arguments.of(
              uniqueKey(Optional.empty(), List.of("a")), uniqueKey(Optional.of("a"), List.of("a"))),
          Arguments.of(
              uniqueKey(Optional.of("a"), List.of("a")),
              uniqueKey(Optional.of("b"), List.of("a"))));
    }

    @ParameterizedTest
    @MethodSource("orderedUniqueKeys")
    void itOrdersByColumnsThenName(final UniqueKey smaller, final UniqueKey larger) {
      assertStrictlyBefore(CatalogOrder.UNIQUE_KEYS, smaller, larger);
    }

    @Test
    void itComparesIdenticalKeysAsZero() {
      assertEquals(
          0,
          CatalogOrder.UNIQUE_KEYS.compare(
              uniqueKey(Optional.of("uq"), List.of("a")),
              uniqueKey(Optional.of("uq"), List.of("a"))));
    }
  }

  @Nested
  class ForeignKeysTests {

    static Stream<Arguments> orderedForeignKeys() {
      final var noAction = ReferentialAction.NO_ACTION;
      final var id = List.of("id");
      final var named = Optional.of("fk");
      return Stream.of(
          // local columns dominate every later key
          Arguments.of(
              foreignKey(
                  List.of("a"),
                  USERS,
                  List.of("z"),
                  ReferentialAction.SET_DEFAULT,
                  ReferentialAction.SET_DEFAULT,
                  Optional.of("z")),
              foreignKey(
                  List.of("b"), ACCOUNTS, List.of("a"), noAction, noAction, Optional.empty())),
          // shorter local column prefix first
          Arguments.of(
              foreignKey(List.of("a"), USERS, id, noAction, noAction, named),
              foreignKey(List.of("a", "b"), USERS, List.of("id", "x"), noAction, noAction, named)),
          // then referenced table
          Arguments.of(
              foreignKey(List.of("a"), ACCOUNTS, List.of("z"), noAction, noAction, named),
              foreignKey(List.of("a"), USERS, List.of("a"), noAction, noAction, named)),
          // then referenced columns
          Arguments.of(
              foreignKey(
                  List.of("a"),
                  USERS,
                  List.of("a"),
                  ReferentialAction.CASCADE,
                  noAction,
                  Optional.of("z")),
              foreignKey(List.of("a"), USERS, List.of("b"), noAction, noAction, named)),
          // then onDelete, in constant order
          Arguments.of(
              foreignKey(
                  List.of("a"),
                  USERS,
                  id,
                  ReferentialAction.RESTRICT,
                  ReferentialAction.SET_DEFAULT,
                  named),
              foreignKey(List.of("a"), USERS, id, ReferentialAction.CASCADE, noAction, named)),
          // then onUpdate
          Arguments.of(
              foreignKey(
                  List.of("a"), USERS, id, noAction, ReferentialAction.SET_NULL, Optional.of("z")),
              foreignKey(
                  List.of("a"),
                  USERS,
                  id,
                  noAction,
                  ReferentialAction.SET_DEFAULT,
                  Optional.of("a"))),
          // then name, absent first
          Arguments.of(
              foreignKey(List.of("a"), USERS, id, noAction, noAction, Optional.empty()),
              foreignKey(List.of("a"), USERS, id, noAction, noAction, Optional.of("a"))),
          Arguments.of(
              foreignKey(List.of("a"), USERS, id, noAction, noAction, Optional.of("a")),
              foreignKey(List.of("a"), USERS, id, noAction, noAction, Optional.of("b"))));
    }

    @ParameterizedTest
    @MethodSource("orderedForeignKeys")
    void itOrdersByEachComponentInTurn(final ForeignKey smaller, final ForeignKey larger) {
      assertStrictlyBefore(CatalogOrder.FOREIGN_KEYS, smaller, larger);
    }

    @Test
    void itComparesIdenticalKeysAsZero() {
      assertEquals(
          0,
          CatalogOrder.FOREIGN_KEYS.compare(
              baseForeignKey(List.of("user_id")), baseForeignKey(List.of("user_id"))));
    }
  }

  @Nested
  class SortedCopyTests {

    @Test
    void itReturnsSortedMutableCopyAndLeavesInputUnchanged() {
      final var input = List.of("c", "a", "b");

      final var sorted = CatalogOrder.sortedCopy(input, Comparator.naturalOrder());
      sorted.add("d");

      assertEquals(List.of("a", "b", "c", "d"), sorted);
      assertEquals(List.of("c", "a", "b"), input);
      assertNotSame(input, sorted);
    }

    @Test
    void itReturnsEmptyCopyForEmptyInput() {
      final var sorted =
          CatalogOrder.sortedCopy(new ArrayList<String>(), Comparator.<String>naturalOrder());

      assertTrue(sorted.isEmpty());
    }
  }
}
