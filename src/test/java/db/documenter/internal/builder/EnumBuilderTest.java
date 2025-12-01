package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.queries.api.QueryRunner;
import java.sql.SQLException;
import java.util.List;
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
              .enumName("status")
              .columnNames(List.of("status"))
              .enumValues(List.of())
              .build();
      final DbEnum dbEnum2 =
          DbEnum.builder()
              .enumName("role")
              .columnNames(List.of("role"))
              .enumValues(List.of())
              .build();

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
              .enumName("empty")
              .columnNames(List.of("empty"))
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
              .enumName("status")
              .columnNames(List.of("status"))
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
}
