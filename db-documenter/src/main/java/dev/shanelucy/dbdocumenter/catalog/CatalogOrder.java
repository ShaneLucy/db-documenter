package dev.shanelucy.dbdocumenter.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The canonical orderings the catalog records sort their lists into.
 *
 * <p>Each comparator is total over valid records, so sorting never depends on input order.
 */
final class CatalogOrder {

  /** Orders unique keys by column list, then name. */
  static final Comparator<UniqueKey> UNIQUE_KEYS =
      Comparator.comparing(UniqueKey::columns, CatalogOrder::compareLists)
          .thenComparing(UniqueKey::name, CatalogOrder::compareOptionalNames);

  /**
   * Orders foreign keys by local columns, referenced table, referenced columns, delete action,
   * update action, then name.
   */
  static final Comparator<ForeignKey> FOREIGN_KEYS =
      Comparator.comparing(ForeignKey::columns, CatalogOrder::compareLists)
          .thenComparing(ForeignKey::referencedTable)
          .thenComparing(ForeignKey::referencedColumns, CatalogOrder::compareLists)
          .thenComparing(ForeignKey::onDelete)
          .thenComparing(ForeignKey::onUpdate)
          .thenComparing(ForeignKey::name, CatalogOrder::compareOptionalNames);

  private CatalogOrder() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Returns a mutable copy of {@code values} sorted by {@code order}.
   *
   * <p>Callers wrap the result in {@link List#copyOf} themselves, which lets SpotBugs see that the
   * record component is immutable. They must have rejected null lists and null elements first.
   */
  static <T> List<T> sortedCopy(final List<T> values, final Comparator<? super T> order) {
    final var copy = new ArrayList<T>(values);
    copy.sort(order);
    return copy;
  }

  /**
   * Compares lists element by element; when one is a prefix of the other, the shorter sorts first.
   */
  static <T extends Comparable<? super T>> int compareLists(
      final List<T> left, final List<T> right) {
    final int commonSize = Math.min(left.size(), right.size());
    for (int index = 0; index < commonSize; index++) {
      final int comparison = left.get(index).compareTo(right.get(index));
      if (comparison != 0) {
        return comparison;
      }
    }
    return Integer.compare(left.size(), right.size());
  }

  /** Compares optional names with an absent name first, then present names in String order. */
  static int compareOptionalNames(final Optional<String> left, final Optional<String> right) {
    if (left.isEmpty()) {
      return right.isEmpty() ? 0 : -1;
    }
    if (right.isEmpty()) {
      return 1;
    }
    return left.orElseThrow().compareTo(right.orElseThrow());
  }
}
