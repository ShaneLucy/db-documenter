package db.documenter.internal.validation;

import db.documenter.internal.exceptions.ValidationException;
import java.util.List;

public final class Validators {

  private Validators() {
    throw new IllegalStateException("Utility class");
  }

  public static void isNotBlank(final String value, final String propertyName) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(propertyName + " must not be blank");
    }
  }

  public static void containsAtLeast1Item(final List<?> value, final String propertyName) {
    if (value == null || value.isEmpty()) {
      throw new ValidationException(propertyName + " must contain at least 1 item");
    }
  }

  public static void isNotNull(final Object value, final String propertyName) {
    if (value == null) {
      throw new ValidationException(propertyName + " must not be null");
    }
  }
}
