package dev.shanelucy.dbdocumenter.internal.validation;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Run-time validation helpers for values NullAway cannot vouch for, such as JDBC results, CLI input
 * and callers of the public API.
 *
 * <p>Each check returns its argument so builders can validate and pass the value on in one step,
 * which also lets NullAway see the result as non-null.
 */
public final class Validators {

  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65_535;

  private Validators() {
    throw new IllegalStateException("Utility class");
  }

  public static String isNotBlank(final @Nullable String value, final String propertyName) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(propertyName + " must not be blank");
    }
    return value;
  }

  /**
   * Ensures {@code value} is non-null and non-empty, but unlike {@link #isNotBlank} accepts
   * whitespace, because database identifiers may legally consist of whitespace.
   *
   * @param value the string to check
   * @param propertyName name used in the failure message
   * @return {@code value}, for chaining
   * @throws ValidationException if the value is null or empty
   */
  public static String isNotEmpty(final @Nullable String value, final String propertyName) {
    if (value == null) {
      throw new ValidationException(propertyName + " must not be null");
    }
    if (value.isEmpty()) {
      throw new ValidationException(propertyName + " must not be empty");
    }
    return value;
  }

  public static <T extends List<?>> T containsAtLeast1Item(
      final @Nullable T value, final String propertyName) {
    if (value == null || value.isEmpty()) {
      throw new ValidationException(propertyName + " must contain at least 1 item");
    }
    return value;
  }

  public static <T> T isNotNull(final @Nullable T value, final String propertyName) {
    if (value == null) {
      throw new ValidationException(propertyName + " must not be null");
    }
    return value;
  }

  public static <T> List<T> containsNoNullElements(
      final @Nullable List<T> value, final String propertyName) {
    if (value == null) {
      throw new ValidationException(propertyName + " must not be null");
    }
    if (value.stream().anyMatch(Objects::isNull)) {
      throw new ValidationException(propertyName + " must not contain null elements");
    }
    return value;
  }

  /**
   * Ensures {@code value} contains no element twice, by {@link Object#equals}.
   *
   * @param value the list to check
   * @param propertyName name used in the failure message, which also names the first duplicate
   * @return {@code value}, for chaining
   * @throws ValidationException if the list is null or contains a duplicate
   */
  public static <T> List<T> containsNoDuplicates(
      final @Nullable List<T> value, final String propertyName) {
    if (value == null) {
      throw new ValidationException(propertyName + " must not be null");
    }
    final var seen = new HashSet<T>();
    for (final T element : value) {
      if (!seen.add(element)) {
        throw new ValidationException(
            propertyName + " must not contain duplicates, found duplicate: " + element);
      }
    }
    return value;
  }

  /**
   * Ensures {@code number} is a valid TCP port, between 1 and 65535 inclusive.
   *
   * @param number the port to check
   * @param propertyName name used in the failure message
   * @return {@code number}, for chaining
   * @throws ValidationException if the port is outside the valid range
   */
  public static int isValidPort(final int number, final String propertyName) {
    if (number < MIN_PORT || number > MAX_PORT) {
      throw new ValidationException(propertyName + " must be between 1 and 65535");
    }
    return number;
  }

  public static int isPositive(final int number, final String propertyName) {
    if (number <= 0) {
      throw new ValidationException(propertyName + " must be positive");
    }
    return number;
  }
}
