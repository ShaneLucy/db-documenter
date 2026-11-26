package db.documenter.internal.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColumnMappingContextTest {

  @Nested
  class ValidationTests {

    @Test
    void itThrowsExceptionWhenColumnUdtMappingsIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new ColumnMappingContext(null, Map.of(), "test_table", "test_schema"));

      assertEquals("columnUdtMappings must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenEnumsByKeyIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new ColumnMappingContext(Map.of(), null, "test_table", "test_schema"));

      assertEquals("enumsByKey must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenCurrentTableNameIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new ColumnMappingContext(Map.of(), Map.of(), null, "test_schema"));

      assertEquals("currentTableName must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenCurrentTableNameIsBlank() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new ColumnMappingContext(Map.of(), Map.of(), "", "test_schema"));

      assertEquals("currentTableName must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenCurrentSchemaIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new ColumnMappingContext(Map.of(), Map.of(), "test_table", null));

      assertEquals("currentSchema must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenCurrentSchemaIsBlank() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new ColumnMappingContext(Map.of(), Map.of(), "test_table", ""));

      assertEquals("currentSchema must not be blank", exception.getMessage());
    }
  }

  @Nested
  class ImmutabilityTests {

    @Test
    void itCreatesDefensiveCopyOfColumnUdtMappings() {
      final var originalMappings = new HashMap<ColumnKey, UdtReference>();
      final var columnKey = new ColumnKey("users", "status");
      final var udtReference = new UdtReference("core", "user_status");
      originalMappings.put(columnKey, udtReference);

      final var context =
          new ColumnMappingContext(originalMappings, Map.of(), "test_table", "test_schema");

      final var newColumnKey = new ColumnKey("orders", "status");
      final var newUdtReference = new UdtReference("core", "order_status");
      originalMappings.put(newColumnKey, newUdtReference);

      assertEquals(1, context.columnUdtMappings().size());
      assertEquals(udtReference, context.columnUdtMappings().get(columnKey));
    }

    @Test
    void itCreatesDefensiveCopyOfEnumsByKey() {
      final var originalEnums = new HashMap<EnumKey, DbEnum>();
      final var enumKey = new EnumKey("core", "user_status");
      final var dbEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("user_status")
              .enumValues(List.of("active", "inactive"))
              .build();
      originalEnums.put(enumKey, dbEnum);

      final var context =
          new ColumnMappingContext(Map.of(), originalEnums, "test_table", "test_schema");

      final var newEnumKey = new EnumKey("core", "order_status");
      final var newDbEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("order_status")
              .enumValues(List.of("pending", "completed"))
              .build();
      originalEnums.put(newEnumKey, newDbEnum);

      assertEquals(1, context.enumsByKey().size());
      assertEquals(dbEnum, context.enumsByKey().get(enumKey));
    }

    @Test
    void itReturnsImmutableColumnUdtMappings() {
      final var columnKey = new ColumnKey("users", "status");
      final var udtReference = new UdtReference("core", "user_status");
      final var context =
          new ColumnMappingContext(
              Map.of(columnKey, udtReference), Map.of(), "test_table", "test_schema");

      assertThrows(
          UnsupportedOperationException.class,
          () ->
              context
                  .columnUdtMappings()
                  .put(
                      new ColumnKey("orders", "status"), new UdtReference("core", "order_status")));
    }

    @Test
    void itReturnsImmutableEnumsByKey() {
      final var enumKey = new EnumKey("core", "user_status");
      final var dbEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("user_status")
              .enumValues(List.of("active", "inactive"))
              .build();
      final var context =
          new ColumnMappingContext(Map.of(), Map.of(enumKey, dbEnum), "test_table", "test_schema");

      assertThrows(
          UnsupportedOperationException.class,
          () ->
              context
                  .enumsByKey()
                  .put(
                      new EnumKey("core", "order_status"),
                      DbEnum.builder()
                          .schemaName("core")
                          .enumName("order_status")
                          .enumValues(List.of("pending"))
                          .build()));
    }
  }

  @Nested
  class ConstructionTests {

    @Test
    void itConstructsWithAllFieldsSet() {
      final var columnKey = new ColumnKey("users", "status");
      final var udtReference = new UdtReference("core", "user_status");
      final var enumKey = new EnumKey("core", "user_status");
      final var dbEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("user_status")
              .enumValues(List.of("active", "inactive"))
              .build();

      final var context =
          new ColumnMappingContext(
              Map.of(columnKey, udtReference), Map.of(enumKey, dbEnum), "users", "core");

      assertEquals(Map.of(columnKey, udtReference), context.columnUdtMappings());
      assertEquals(Map.of(enumKey, dbEnum), context.enumsByKey());
      assertEquals("users", context.currentTableName());
      assertEquals("core", context.currentSchema());
    }

    @Test
    void itConstructsWithEmptyMaps() {
      final var context = new ColumnMappingContext(Map.of(), Map.of(), "test_table", "test_schema");

      assertEquals(Map.of(), context.columnUdtMappings());
      assertEquals(Map.of(), context.enumsByKey());
      assertEquals("test_table", context.currentTableName());
      assertEquals("test_schema", context.currentSchema());
    }
  }
}
