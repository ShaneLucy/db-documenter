package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DataTypeTest {

  @Nested
  class ConstructorTests {

    @Test
    void itAcceptsEmptyDisplayText() {
      final var dataType = new DataType("", Optional.empty());

      assertEquals("", dataType.displayText());
    }

    @Test
    void itThrowsExceptionWhenDisplayTextIsNull() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new DataType(null, Optional.empty()));

      assertEquals("displayText must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenUserTypeOptionalIsNull() {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new DataType("int4", null));

      assertEquals("userType must not be null", exception.getMessage());
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsBuiltInTypeWithoutUserType() {
      final var dataType = DataType.builder().displayText("varchar(20)").build();

      assertEquals("varchar(20)", dataType.displayText());
      assertTrue(dataType.userType().isEmpty());
    }

    @Test
    void itBuildsUserDefinedType() {
      final var mood = QualifiedName.of("app", "mood");

      final var dataType = DataType.builder().displayText("mood[]").userType(mood).build();

      assertEquals("mood[]", dataType.displayText());
      assertEquals(Optional.of(mood), dataType.userType());
    }

    @Test
    void itTreatsNullUserTypeAsAbsent() {
      final var dataType = DataType.builder().displayText("int4").userType(null).build();

      assertTrue(dataType.userType().isEmpty());
    }

    @Test
    void itThrowsExceptionWhenDisplayTextIsNotSet() {
      final var builder = DataType.builder();

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("displayText must not be null", exception.getMessage());
    }
  }
}
