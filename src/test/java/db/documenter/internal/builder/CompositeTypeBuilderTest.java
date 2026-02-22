package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import db.documenter.internal.models.db.CompositeField;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompositeTypeBuilderTest {

  @Mock private PostgresqlQueryRunner queryRunner;

  private CompositeTypeBuilder compositeTypeBuilder;

  @BeforeEach
  void setUp() {
    reset(queryRunner);
    compositeTypeBuilder = new CompositeTypeBuilder();
  }

  @Nested
  class BuildCompositeTypesTests {

    @Test
    void returnsEmptyListWhenNoCompositeTypesExist() throws SQLException {
      when(queryRunner.getCompositeTypeInfo("test_schema")).thenReturn(List.of());

      final List<DbCompositeType> result =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "test_schema");

      assertTrue(result.isEmpty());
    }

    @Test
    void buildsSingleCompositeTypeWithFields() throws SQLException {
      final var field1 =
          CompositeField.builder()
              .fieldName("street")
              .fieldType("character varying(200)")
              .position(1)
              .build();
      final var field2 =
          CompositeField.builder()
              .fieldName("city")
              .fieldType("character varying(100)")
              .position(2)
              .build();

      final var dbCompositeType =
          DbCompositeType.builder()
              .typeName("address")
              .schemaName("test_schema")
              .fields(List.of(field1, field2))
              .build();

      when(queryRunner.getCompositeTypeInfo("test_schema")).thenReturn(List.of(dbCompositeType));

      final List<DbCompositeType> result =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "test_schema");

      assertEquals(1, result.size());
      assertEquals("address", result.getFirst().typeName());
      assertEquals("test_schema", result.getFirst().schemaName());
      assertEquals(2, result.getFirst().fields().size());
      assertEquals("street", result.getFirst().fields().getFirst().fieldName());
      assertEquals("character varying(200)", result.getFirst().fields().getFirst().fieldType());
      assertEquals("city", result.getFirst().fields().get(1).fieldName());
      assertEquals("character varying(100)", result.getFirst().fields().get(1).fieldType());
    }

    @Test
    void buildsMultipleCompositeTypes() throws SQLException {
      final var addressField1 =
          CompositeField.builder().fieldName("street").fieldType("text").position(1).build();
      final var addressField2 =
          CompositeField.builder().fieldName("city").fieldType("text").position(2).build();

      final var moneyField1 =
          CompositeField.builder()
              .fieldName("amount")
              .fieldType("numeric(10,2)")
              .position(1)
              .build();
      final var moneyField2 =
          CompositeField.builder()
              .fieldName("currency_code")
              .fieldType("character(3)")
              .position(2)
              .build();

      final var dbCompositeType1 =
          DbCompositeType.builder()
              .typeName("address")
              .schemaName("test_schema")
              .fields(List.of(addressField1, addressField2))
              .build();

      final var dbCompositeType2 =
          DbCompositeType.builder()
              .typeName("money_amount")
              .schemaName("test_schema")
              .fields(List.of(moneyField1, moneyField2))
              .build();

      when(queryRunner.getCompositeTypeInfo("test_schema"))
          .thenReturn(List.of(dbCompositeType1, dbCompositeType2));

      final List<DbCompositeType> result =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "test_schema");

      assertEquals(2, result.size());
      assertEquals("address", result.getFirst().typeName());
      assertEquals(2, result.getFirst().fields().size());
      assertEquals("money_amount", result.get(1).typeName());
      assertEquals(2, result.get(1).fields().size());
    }

    @Test
    void buildsCompositeTypeWithEmptyFields() throws SQLException {
      final var dbCompositeType =
          DbCompositeType.builder()
              .typeName("empty_type")
              .schemaName("test_schema")
              .fields(List.of())
              .build();

      when(queryRunner.getCompositeTypeInfo("test_schema")).thenReturn(List.of(dbCompositeType));

      final List<DbCompositeType> result =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "test_schema");

      assertEquals(1, result.size());
      assertEquals("empty_type", result.getFirst().typeName());
      assertTrue(result.getFirst().fields().isEmpty());
    }

    @Test
    void propagatesSQLExceptionFromGetCompositeTypeInfo() throws SQLException {
      when(queryRunner.getCompositeTypeInfo("test_schema"))
          .thenThrow(new SQLException("Connection failed"));

      final SQLException exception =
          assertThrows(
              SQLException.class,
              () -> compositeTypeBuilder.buildCompositeTypes(queryRunner, "test_schema"));

      assertEquals("Connection failed", exception.getMessage());
    }

    @Test
    void buildsCompositeTypeWithVariousPostgresqlTypes() throws SQLException {
      final var field1 =
          CompositeField.builder().fieldName("id").fieldType("uuid").position(1).build();
      final var field2 =
          CompositeField.builder().fieldName("name").fieldType("text").position(2).build();
      final var field3 =
          CompositeField.builder().fieldName("age").fieldType("integer").position(3).build();
      final var field4 =
          CompositeField.builder()
              .fieldName("created_at")
              .fieldType("timestamp without time zone")
              .position(4)
              .build();

      final var dbCompositeType =
          DbCompositeType.builder()
              .typeName("person_record")
              .schemaName("test_schema")
              .fields(List.of(field1, field2, field3, field4))
              .build();

      when(queryRunner.getCompositeTypeInfo("test_schema")).thenReturn(List.of(dbCompositeType));

      final List<DbCompositeType> result =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "test_schema");

      assertEquals(1, result.size());
      assertEquals("person_record", result.getFirst().typeName());
      assertEquals(4, result.getFirst().fields().size());
      assertEquals("uuid", result.getFirst().fields().getFirst().fieldType());
      assertEquals("text", result.getFirst().fields().get(1).fieldType());
      assertEquals("integer", result.getFirst().fields().get(2).fieldType());
      assertEquals("timestamp without time zone", result.getFirst().fields().get(3).fieldType());
    }

    @Test
    void preservesSchemaNameInCompositeTypes() throws SQLException {
      final var field =
          CompositeField.builder().fieldName("value").fieldType("text").position(1).build();

      final var coreCompositeType =
          DbCompositeType.builder()
              .typeName("test_type")
              .schemaName("core")
              .fields(List.of(field))
              .build();

      when(queryRunner.getCompositeTypeInfo("core")).thenReturn(List.of(coreCompositeType));

      final List<DbCompositeType> result =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "core");

      assertEquals(1, result.size());
      assertEquals("core", result.getFirst().schemaName());
    }

    @Test
    void buildsCompositeTypesWithDifferentSchemasDistinctly() throws SQLException {
      final var coreField =
          CompositeField.builder().fieldName("value1").fieldType("text").position(1).build();
      final var auditField =
          CompositeField.builder().fieldName("value2").fieldType("text").position(1).build();

      final var coreCompositeType =
          DbCompositeType.builder()
              .typeName("shared_type")
              .schemaName("core")
              .fields(List.of(coreField))
              .build();

      final var auditCompositeType =
          DbCompositeType.builder()
              .typeName("shared_type")
              .schemaName("audit")
              .fields(List.of(auditField))
              .build();

      when(queryRunner.getCompositeTypeInfo("core")).thenReturn(List.of(coreCompositeType));
      when(queryRunner.getCompositeTypeInfo("audit")).thenReturn(List.of(auditCompositeType));

      final List<DbCompositeType> coreResult =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "core");
      final List<DbCompositeType> auditResult =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "audit");

      assertEquals(1, coreResult.size());
      assertEquals("core", coreResult.getFirst().schemaName());
      assertEquals("value1", coreResult.getFirst().fields().getFirst().fieldName());

      assertEquals(1, auditResult.size());
      assertEquals("audit", auditResult.getFirst().schemaName());
      assertEquals("value2", auditResult.getFirst().fields().getFirst().fieldName());
    }

    @Test
    void buildsCompositeTypesWithManyFields() throws SQLException {
      final var fields =
          List.of(
              CompositeField.builder().fieldName("field1").fieldType("text").position(1).build(),
              CompositeField.builder().fieldName("field2").fieldType("integer").position(2).build(),
              CompositeField.builder().fieldName("field3").fieldType("boolean").position(3).build(),
              CompositeField.builder()
                  .fieldName("field4")
                  .fieldType("timestamp")
                  .position(4)
                  .build(),
              CompositeField.builder().fieldName("field5").fieldType("uuid").position(5).build());

      final var dbCompositeType =
          DbCompositeType.builder()
              .typeName("complex_type")
              .schemaName("test_schema")
              .fields(fields)
              .build();

      when(queryRunner.getCompositeTypeInfo("test_schema")).thenReturn(List.of(dbCompositeType));

      final List<DbCompositeType> result =
          compositeTypeBuilder.buildCompositeTypes(queryRunner, "test_schema");

      assertEquals(1, result.size());
      assertEquals("complex_type", result.getFirst().typeName());
      assertEquals(5, result.getFirst().fields().size());
    }
  }
}
