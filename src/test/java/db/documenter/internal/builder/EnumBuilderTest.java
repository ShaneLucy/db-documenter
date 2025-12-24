package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.queries.api.QueryRunner;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnumBuilderTest {

  @Mock private QueryRunner queryRunner;

  private EnumBuilder enumBuilder;

  @BeforeEach
  void setUp() {
    reset(queryRunner);
    enumBuilder = new EnumBuilder();
  }

  @Nested
  class BuildEnumsTests {

    @Test
    void returnsEmptyListWhenNoEnumsExist() throws SQLException {
      when(queryRunner.getEnumInfo("test_schema")).thenReturn(List.of());

      final List<DbEnum> result = enumBuilder.buildEnums(queryRunner, "test_schema");

      assertTrue(result.isEmpty());
    }

    @Test
    void buildsEnumsWithValues() throws SQLException {
      final DbEnum dbEnum1 =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of())
              .build();
      final DbEnum dbEnum2 =
          DbEnum.builder().schemaName("test_schema").enumName("role").enumValues(List.of()).build();

      when(queryRunner.getEnumInfo("test_schema")).thenReturn(List.of(dbEnum1, dbEnum2));
      when(queryRunner.getEnumValues("test_schema", dbEnum1))
          .thenReturn(List.of("active", "inactive"));
      when(queryRunner.getEnumValues("test_schema", dbEnum2)).thenReturn(List.of("admin", "user"));

      final List<DbEnum> result = enumBuilder.buildEnums(queryRunner, "test_schema");

      assertEquals(2, result.size());
      assertEquals("status", result.getFirst().enumName());
      assertEquals(List.of("active", "inactive"), result.getFirst().enumValues());
      assertEquals("role", result.get(1).enumName());
      assertEquals(List.of("admin", "user"), result.get(1).enumValues());
    }

    @Test
    void buildsEnumWithEmptyValues() throws SQLException {
      final DbEnum dbEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("empty")
              .enumValues(List.of())
              .build();

      when(queryRunner.getEnumInfo("test_schema")).thenReturn(List.of(dbEnum));
      when(queryRunner.getEnumValues("test_schema", dbEnum)).thenReturn(List.of());

      final List<DbEnum> result = enumBuilder.buildEnums(queryRunner, "test_schema");

      assertEquals(1, result.size());
      assertTrue(result.getFirst().enumValues().isEmpty());
    }

    @Test
    void propagatesSQLExceptionFromGetEnumInfo() throws SQLException {
      when(queryRunner.getEnumInfo("test_schema")).thenThrow(new SQLException("Connection failed"));

      final SQLException exception =
          assertThrows(
              SQLException.class, () -> enumBuilder.buildEnums(queryRunner, "test_schema"));

      assertEquals("Connection failed", exception.getMessage());
    }

    @Test
    void propagatesSQLExceptionFromGetEnumValues() throws SQLException {
      final DbEnum dbEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of())
              .build();

      when(queryRunner.getEnumInfo("test_schema")).thenReturn(List.of(dbEnum));
      when(queryRunner.getEnumValues("test_schema", dbEnum))
          .thenThrow(new SQLException("Failed to fetch values"));

      final SQLException exception =
          assertThrows(
              SQLException.class, () -> enumBuilder.buildEnums(queryRunner, "test_schema"));

      assertEquals("Failed to fetch values", exception.getMessage());
    }
  }

  @Nested
  class BuildEnumKeysTests {

    @Test
    void returnsEmptyMapWhenListIsEmpty() {
      final Map<EnumKey, DbEnum> result = enumBuilder.buildEnumKeys(List.of(), "test_schema");

      assertTrue(result.isEmpty());
    }

    @Test
    void createsSingleEnumKeyToDbEnumMapping() {
      final var dbEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of("active", "inactive"))
              .build();

      final Map<EnumKey, DbEnum> result = enumBuilder.buildEnumKeys(List.of(dbEnum), "test_schema");

      assertEquals(1, result.size());
      final var expectedKey = new EnumKey("test_schema", "status");
      assertTrue(result.containsKey(expectedKey));
      assertEquals(dbEnum, result.get(expectedKey));
    }

    @Test
    void createsMultipleEnumKeysFromSameSchema() {
      final var statusEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of("active", "inactive"))
              .build();
      final var roleEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("role")
              .enumValues(List.of("admin", "user"))
              .build();

      final Map<EnumKey, DbEnum> result =
          enumBuilder.buildEnumKeys(List.of(statusEnum, roleEnum), "test_schema");

      assertEquals(2, result.size());
      final var statusKey = new EnumKey("test_schema", "status");
      final var roleKey = new EnumKey("test_schema", "role");
      assertTrue(result.containsKey(statusKey));
      assertTrue(result.containsKey(roleKey));
      assertEquals(statusEnum, result.get(statusKey));
      assertEquals(roleEnum, result.get(roleKey));
    }

    @Test
    void createsDifferentKeysForSameEnumNameInDifferentSchemas() {
      final var coreStatusEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("project_status")
              .enumValues(List.of("ACTIVE", "COMPLETED", "CANCELLED"))
              .build();
      final var auditStatusEnum =
          DbEnum.builder()
              .schemaName("audit")
              .enumName("project_status")
              .enumValues(List.of("PENDING", "APPROVED", "REJECTED"))
              .build();

      final Map<EnumKey, DbEnum> result =
          enumBuilder.buildEnumKeys(List.of(coreStatusEnum, auditStatusEnum), "core");

      assertEquals(2, result.size());
      final var coreKey = new EnumKey("core", "project_status");
      final var auditKey = new EnumKey("audit", "project_status");
      assertTrue(result.containsKey(coreKey));
      assertTrue(result.containsKey(auditKey));
      assertEquals(coreStatusEnum, result.get(coreKey));
      assertEquals(auditStatusEnum, result.get(auditKey));
    }

    @Test
    void throwsIllegalStateExceptionWhenDuplicateKeysExist() {
      final var firstEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of("active", "inactive"))
              .build();
      final var duplicateEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of("pending", "completed"))
              .build();

      final var exception =
          assertThrows(
              IllegalStateException.class,
              () -> enumBuilder.buildEnumKeys(List.of(firstEnum, duplicateEnum), "test_schema"));

      assertTrue(exception.getMessage().contains("Duplicate key"));
    }
  }
}
