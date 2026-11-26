package db.documenter.internal.validation;

import db.documenter.internal.exceptions.ValidationException;
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

  private Validators() {
    throw new IllegalStateException("Utility class");
  }

  public static String isNotBlank(final @Nullable String value, final String propertyName) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(propertyName + " must not be blank");
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

  public static int isPositive(final int number, final String propertyName) {
    if (number <= 0) {
      throw new ValidationException(propertyName + " must be positive");
    }
    return number;
  }
}
