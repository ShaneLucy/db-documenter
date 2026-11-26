package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Validation shared by several catalog records. */
final class CatalogValidation {

  private CatalogValidation() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Ensures {@code columns} is a non-empty list of non-empty column names.
   *
   * @throws ValidationException if the list is null or empty, or contains a null or empty name
   */
  static void validateColumnNames(final List<String> columns, final String propertyName) {
    Validators.containsAtLeast1Item(columns, propertyName);
    Validators.containsNoNullElements(columns, propertyName);
    columns.forEach(column -> Validators.isNotEmpty(column, propertyName));
  }

  /**
   * Ensures an optional constraint name is non-null and, when present, non-empty.
   *
   * @throws ValidationException if the optional is null or holds an empty name
   */
  static void validateOptionalName(final Optional<String> name, final String propertyName) {
    Validators.isNotNull(name, propertyName);
    name.ifPresent(value -> Validators.isNotEmpty(value, propertyName));
  }

  /**
   * Ensures no two elements share a key, such as two tables with the same name.
   *
   * @throws ValidationException naming the duplicate key and {@code propertyName}
   */
  static <T, K> void validateUniqueKeys(
      final List<T> elements, final Function<T, K> key, final String propertyName) {
    Validators.containsNoDuplicates(elements.stream().map(key).toList(), propertyName);
  }

  /**
   * Ensures every name in {@code columns} is one of {@code tableColumns}.
   *
   * @throws ValidationException naming the unknown column and {@code propertyName}
   */
  static void validateColumnsExist(
      final List<String> columns,
      final Collection<String> tableColumns,
      final String propertyName) {
    for (final String column : columns) {
      if (!tableColumns.contains(column)) {
        throw new ValidationException(
            propertyName + " refers to column " + column + ", which is not in the table");
      }
    }
  }
}
