package db.documenter.internal.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import db.documenter.internal.exceptions.ValidationException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidatorsTest {

  @Nested
  class IsNotBlankTests {

    @ParameterizedTest
    @ValueSource(strings = {"test", "test test", " test", " test ", " test test", " test test "})
    void itDoesNotThrowExceptionWhenStringIsValid(final String input) {
      assertDoesNotThrow(() -> Validators.isNotBlank(input, "test"));
    }

    @Test
    void itThrowsExceptionWhenStringIsNull() {
      assertThrows(ValidationException.class, () -> Validators.isNotBlank(null, "test"));
    }

    @Test
    void itThrowsExceptionWhenStringIsBlank() {
      assertThrows(ValidationException.class, () -> Validators.isNotBlank("", "test"));
    }
  }

  @Nested
  class ContainsAtLeast1ItemTests {

    @Test
    void itDoesNotThrowExceptionWhenAListIsNotEmpty() {
      assertDoesNotThrow(() -> Validators.containsAtLeast1Item(List.of(""), "test"));
    }

    @Test
    void itThrowsExceptionWhenListIsNull() {
      assertThrows(ValidationException.class, () -> Validators.containsAtLeast1Item(null, "test"));
    }

    @Test
    void itThrowsExceptionWhenListIsEmpty() {
      final var emptyList = Collections.emptyList();
      assertThrows(
          ValidationException.class, () -> Validators.containsAtLeast1Item(emptyList, "test"));
    }
  }

  @Nested
  class IsNotNullTests {

    @Test
    void itDoesNotThrowExceptionWhenValueIsNotNull() {
      assertDoesNotThrow(() -> Validators.isNotNull(new Object(), "test"));
    }

    @Test
    void itThrowsExceptionWhenValueIsNull() {
      assertThrows(ValidationException.class, () -> Validators.isNotNull(null, "test"));
    }
  }
}
