package db.documenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.models.db.postgresql.EnumKey;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnumKeyTest {

  @Nested
  class ConstructorValidationTests {

    @Test
    void itCreatesEnumKeyWithValidSchemaAndName() {
      final var enumKey = new EnumKey("core", "project_status");

      assertEquals("core", enumKey.schema());
      assertEquals("project_status", enumKey.name());
    }

    @Test
    void itThrowsExceptionWhenSchemaIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> new EnumKey(null, "project_status"));

      assertEquals("schema must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemaIsBlank() {
      final var exception =
          assertThrows(ValidationException.class, () -> new EnumKey("", "project_status"));

      assertEquals("schema must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemaIsWhitespace() {
      final var exception =
          assertThrows(ValidationException.class, () -> new EnumKey("  ", "project_status"));

      assertEquals("schema must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> new EnumKey("core", null));

      assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsBlank() {
      final var exception = assertThrows(ValidationException.class, () -> new EnumKey("core", ""));

      assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenNameIsWhitespace() {
      final var exception =
          assertThrows(ValidationException.class, () -> new EnumKey("core", "   "));

      assertEquals("name must not be blank", exception.getMessage());
    }
  }

  @Nested
  class EqualityTests {

    @Test
    void whenSchemaAndNameMatchThenEnumKeysAreEqual() {
      final var enumKey1 = new EnumKey("core", "project_status");
      final var enumKey2 = new EnumKey("core", "project_status");

      assertEquals(enumKey1, enumKey2);
      assertEquals(enumKey1.hashCode(), enumKey2.hashCode());
    }

    @Test
    void whenSchemaDiffersButNameMatchesThenEnumKeysAreNotEqual() {
      final var enumKey1 = new EnumKey("core", "project_status");
      final var enumKey2 = new EnumKey("audit", "project_status");

      assertNotEquals(enumKey1, enumKey2);
    }

    @Test
    void whenNameDiffersButSchemaMatchesThenEnumKeysAreNotEqual() {
      final var enumKey1 = new EnumKey("core", "project_status");
      final var enumKey2 = new EnumKey("core", "order_status");

      assertNotEquals(enumKey1, enumKey2);
    }

    @Test
    void whenBothSchemaAndNameDifferThenEnumKeysAreNotEqual() {
      final var enumKey1 = new EnumKey("core", "project_status");
      final var enumKey2 = new EnumKey("audit", "order_status");

      assertNotEquals(enumKey1, enumKey2);
    }

    @Test
    void whenComparedToNullThenEnumKeysAreNotEqual() {
      final var enumKey = new EnumKey("core", "project_status");

      assertNotEquals(null, enumKey);
    }

    @Test
    void whenComparedToOtherObjectTypeThenEnumKeysAreNotEqual() {
      final var enumKey = new EnumKey("core", "project_status");
      final var otherObject = "core.project_status";

      assertNotEquals(enumKey, otherObject);
    }

    @Test
    void whenComparedToItselfThenEnumKeyIsEqual() {
      final var enumKey = new EnumKey("core", "project_status");

      assertEquals(enumKey, enumKey);
    }
  }

  @Nested
  class HashCodeTests {

    @Test
    void whenSchemaAndNameMatchThenHashCodesAreEqual() {
      final var enumKey1 = new EnumKey("core", "project_status");
      final var enumKey2 = new EnumKey("core", "project_status");

      assertEquals(enumKey1.hashCode(), enumKey2.hashCode());
    }

    @Test
    void whenSchemaDiffersThenHashCodesAreDifferent() {
      final var enumKey1 = new EnumKey("core", "project_status");
      final var enumKey2 = new EnumKey("audit", "project_status");

      assertNotEquals(enumKey1.hashCode(), enumKey2.hashCode());
    }

    @Test
    void whenNameDiffersThenHashCodesAreDifferent() {
      final var enumKey1 = new EnumKey("core", "project_status");
      final var enumKey2 = new EnumKey("core", "order_status");

      assertNotEquals(enumKey1.hashCode(), enumKey2.hashCode());
    }

    @Test
    void hashCodeIsConsistent() {
      final var enumKey = new EnumKey("core", "project_status");
      final var firstHashCode = enumKey.hashCode();
      final var secondHashCode = enumKey.hashCode();

      assertEquals(firstHashCode, secondHashCode);
    }
  }

  @Nested
  class ToStringTests {

    @Test
    void toStringIncludesSchemaAndName() {
      final var enumKey = new EnumKey("core", "project_status");
      final var result = enumKey.toString();

      assertTrue(result.contains("core"));
      assertTrue(result.contains("project_status"));
    }
  }

  @Nested
  class MultiSchemaScenarioTests {

    @Test
    void whenDifferentSchemasHaveSameEnumNameThenKeysAreDistinct() {
      final var coreStatus = new EnumKey("core", "project_status");
      final var auditStatus = new EnumKey("audit", "project_status");

      assertNotEquals(
          coreStatus, auditStatus, "Enums with same name in different schemas should be distinct");
      assertNotEquals(
          coreStatus.hashCode(),
          auditStatus.hashCode(),
          "Hash codes should differ for enums with same name in different schemas");
    }

    @Test
    void whenSameSchemaHasDifferentEnumNamesThenKeysAreDistinct() {
      final var projectStatus = new EnumKey("core", "project_status");
      final var orderStatus = new EnumKey("core", "order_status");

      assertNotEquals(
          projectStatus, orderStatus, "Different enum names in same schema should be distinct");
      assertNotEquals(
          projectStatus.hashCode(),
          orderStatus.hashCode(),
          "Hash codes should differ for different enum names");
    }
  }
}
