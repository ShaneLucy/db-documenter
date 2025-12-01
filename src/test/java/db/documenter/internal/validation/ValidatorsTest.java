package db.documenter.internal.validation;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.exceptions.ValidationException;
import java.util.ArrayList;
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
      final var exception =
          assertThrows(ValidationException.class, () -> Validators.isNotBlank("", "test"));

      assertEquals("test must not be blank", exception.getMessage());
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
      final var exception =
          assertThrows(
              ValidationException.class, () -> Validators.containsAtLeast1Item(null, "test"));
      assertEquals("test must contain at least 1 item", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenListIsEmpty() {
      final var emptyList = Collections.emptyList();
      final var exception =
          assertThrows(
              ValidationException.class, () -> Validators.containsAtLeast1Item(emptyList, "test"));

      assertEquals("test must contain at least 1 item", exception.getMessage());
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
      final var exception =
          assertThrows(ValidationException.class, () -> Validators.isNotNull(null, "test"));

      assertEquals("test must not be null", exception.getMessage());
    }
  }

  @Nested
  class ContainsNoNullElementsTests {

    @Test
    void itDoesNotThrowExceptionWhenListContainsNoNullElements() {

      assertDoesNotThrow(
          () -> Validators.containsNoNullElements(List.of("one", "two"), "propName"));
    }

    @Test
    void itDoesThrowExceptionWhenListContainsNullElements() {
      final List<String> list = new ArrayList<>();
      list.add("");
      list.add("item2");
      list.add(null);
      list.add("item3");

      final var exception =
          assertThrows(
              ValidationException.class, () -> Validators.containsNoNullElements(list, "propName"));

      assertEquals("propName must not contain null elements", exception.getMessage());
    }
  }
}
