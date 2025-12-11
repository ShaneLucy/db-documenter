package db.documenter.internal.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColumnMapperTest {

  private ColumnMapper columnMapper;

  @BeforeEach
  void setUp() {
    columnMapper = new ColumnMapper();
  }

  private ColumnMappingContext createContext(
      final Map<ColumnKey, UdtReference> columnUdtMappings,
      final Map<EnumKey, DbEnum> enumsByKey,
      final String tableName,
      final String schema) {
    return new ColumnMappingContext(columnUdtMappings, enumsByKey, tableName, schema);
  }

  @Nested
  class MapUserDefinedTypesTests {

    @Test
    void returnsColumnsUnchangedWhenNoUserDefinedTypes() {
      final Column column1 =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final Column column2 =
          Column.builder().name("name").dataType("varchar").constraints(List.of()).build();

      final var context = createContext(Map.of(), Map.of(), "test_table", "core");

      final List<Column> result =
          columnMapper.mapUserDefinedTypes(List.of(column1, column2), context);

      assertEquals(2, result.size());
      assertEquals("uuid", result.getFirst().dataType());
      assertEquals("varchar", result.get(1).dataType());
    }

    @Test
    void mapsUserDefinedTypeToEnumType() {
      final DbEnum dbEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("order_status")
              .enumValues(List.of())
              .build();

      final Column column =
          Column.builder().name("status").dataType("USER-DEFINED").constraints(List.of()).build();

      final var columnKey = new ColumnKey("orders", "status");
      final var udtReference = new UdtReference("core", "order_status");
      final var enumKey = new EnumKey("core", "order_status");

      final var context =
          createContext(Map.of(columnKey, udtReference), Map.of(enumKey, dbEnum), "orders", "core");

      final List<Column> result = columnMapper.mapUserDefinedTypes(List.of(column), context);

      assertEquals(1, result.size());
      assertEquals("order_status", result.getFirst().dataType());
      assertEquals("status", result.getFirst().name());
    }

    @Test
    void keepsUserDefinedTypeWhenNoMatchingEnum() {
      final DbEnum dbEnum =
          DbEnum.builder().schemaName("core").enumName("other_enum").enumValues(List.of()).build();

      final Column column =
          Column.builder().name("status").dataType("USER-DEFINED").constraints(List.of()).build();

      final var enumKey = new EnumKey("core", "other_enum");
      final var context = createContext(Map.of(), Map.of(enumKey, dbEnum), "orders", "core");

      final List<Column> result = columnMapper.mapUserDefinedTypes(List.of(column), context);

      assertEquals(1, result.size());
      assertEquals("USER-DEFINED", result.getFirst().dataType());
    }

    @Test
    void preservesOtherColumnPropertiesWhenMappingTypes() {
      final DbEnum dbEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("order_status")
              .enumValues(List.of())
              .build();

      final Column column =
          Column.builder()
              .name("status")
              .dataType("USER-DEFINED")
              .maximumLength(50)
              .constraints(List.of(Constraint.DEFAULT, Constraint.NULLABLE))
              .build();

      final var columnKey = new ColumnKey("orders", "status");
      final var udtReference = new UdtReference("core", "order_status");
      final var enumKey = new EnumKey("core", "order_status");

      final var context =
          createContext(Map.of(columnKey, udtReference), Map.of(enumKey, dbEnum), "orders", "core");

      final List<Column> result = columnMapper.mapUserDefinedTypes(List.of(column), context);

      assertEquals(1, result.size());
      final var mapped = result.getFirst();
      assertEquals("status", mapped.name());
      assertTrue(mapped.isNullable());
      assertEquals("order_status", mapped.dataType());
      assertEquals(50, mapped.maximumLength());
      assertEquals(List.of(Constraint.DEFAULT, Constraint.NULLABLE), mapped.constraints());
    }

    @Test
    void mapsUserDefinedTypeToEnumInSameSchemaWithoutQualification() {
      final DbEnum publicEnum =
          DbEnum.builder()
              .schemaName("public")
              .enumName("project_status")
              .enumValues(List.of())
              .build();

      final DbEnum coreEnum =
          DbEnum.builder()
              .schemaName("core")
              .enumName("project_status")
              .enumValues(List.of())
              .build();

      final Column column =
          Column.builder().name("status").dataType("USER-DEFINED").constraints(List.of()).build();

      final var columnKey = new ColumnKey("projects", "status");
      final var udtReference = new UdtReference("core", "project_status");
      final var publicEnumKey = new EnumKey("public", "project_status");
      final var coreEnumKey = new EnumKey("core", "project_status");

      final var context =
          createContext(
              Map.of(columnKey, udtReference),
              Map.of(publicEnumKey, publicEnum, coreEnumKey, coreEnum),
              "projects",
              "core");

      final List<Column> result = columnMapper.mapUserDefinedTypes(List.of(column), context);

      assertEquals(1, result.size());
      assertEquals(
          "project_status",
          result.getFirst().dataType(),
          "Should use unqualified name when enum is in same schema");
    }

    @Test
    void mapsUserDefinedTypeToEnumInDifferentSchemaWithQualification() {
      final DbEnum authEnum =
          DbEnum.builder()
              .schemaName("auth")
              .enumName("account_status")
              .enumValues(List.of())
              .build();

      final Column column =
          Column.builder()
              .name("account_status")
              .dataType("USER-DEFINED")
              .constraints(List.of())
              .build();

      final var columnKey = new ColumnKey("users", "account_status");
      final var udtReference = new UdtReference("auth", "account_status");
      final var enumKey = new EnumKey("auth", "account_status");

      final var context =
          createContext(
              Map.of(columnKey, udtReference), Map.of(enumKey, authEnum), "users", "core");

      final List<Column> result = columnMapper.mapUserDefinedTypes(List.of(column), context);

      assertEquals(1, result.size());
      assertEquals(
          "auth.account_status",
          result.getFirst().dataType(),
          "Should use qualified name when enum is in different schema");
    }
  }

  @Nested
  class EnrichWithForeignKeyConstraintsTests {

    @Test
    void returnsColumnsUnchangedWhenNoForeignKeys() {
      final Column column1 =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final Column column2 =
          Column.builder().name("name").dataType("varchar").constraints(List.of()).build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column1, column2), List.of());

      assertEquals(2, result.size());
      assertEquals("id", result.getFirst().name());
      assertEquals("name", result.get(1).name());
      assertTrue(
          result.getFirst().constraints() == null || result.getFirst().constraints().isEmpty());
      assertTrue(result.get(1).constraints() == null || result.get(1).constraints().isEmpty());
    }

    @Test
    void addsFKConstraintToForeignKeyColumn() {
      final Column column =
          Column.builder().name("user_id").dataType("uuid").constraints(List.of()).build();

      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      assertEquals("user_id", result.getFirst().name());
      assertEquals(1, result.getFirst().constraints().size());
      assertEquals(Constraint.FK, result.getFirst().constraints().getFirst());
    }

    @Test
    void addsFKConstraintBeforeExistingConstraints() {
      final Column column =
          Column.builder()
              .name("user_id")
              .dataType("uuid")
              .constraints(List.of(Constraint.UNIQUE, Constraint.DEFAULT))
              .build();

      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      assertEquals(3, result.getFirst().constraints().size());
      assertEquals(Constraint.FK, result.getFirst().constraints().getFirst());
      assertEquals(Constraint.UNIQUE, result.getFirst().constraints().get(1));
      assertEquals(Constraint.DEFAULT, result.getFirst().constraints().get(2));
    }

    @Test
    void handlesMultipleForeignKeyColumns() {
      final Column column1 =
          Column.builder().name("user_id").dataType("uuid").constraints(List.of()).build();
      final Column column2 =
          Column.builder().name("product_id").dataType("bigint").constraints(List.of()).build();
      final Column column3 =
          Column.builder().name("name").dataType("varchar").constraints(List.of()).build();

      final ForeignKey foreignKey1 =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final ForeignKey foreignKey2 =
          ForeignKey.builder()
              .name("fk_product")
              .sourceTable("orders")
              .sourceColumn("product_id")
              .targetTable("products")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(
              List.of(column1, column2, column3), List.of(foreignKey1, foreignKey2));

      assertEquals(3, result.size());

      // user_id should have FK
      assertEquals("user_id", result.getFirst().name());
      assertEquals(1, result.getFirst().constraints().size());
      assertEquals(Constraint.FK, result.getFirst().constraints().getFirst());

      // product_id should have FK
      assertEquals("product_id", result.get(1).name());
      assertEquals(1, result.get(1).constraints().size());
      assertEquals(Constraint.FK, result.get(1).constraints().getFirst());

      // name should not have FK
      assertEquals("name", result.get(2).name());
      assertTrue(result.get(2).constraints() == null || result.get(2).constraints().isEmpty());
    }

    @Test
    void isCaseInsensitiveWhenMatchingColumnNames() {
      final Column column =
          Column.builder().name("USER_ID").dataType("uuid").constraints(List.of()).build();

      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      assertEquals("USER_ID", result.getFirst().name());
      assertFalse(result.getFirst().constraints().isEmpty());
      assertEquals(Constraint.FK, result.getFirst().constraints().getFirst());
    }

    @Test
    void preservesOtherColumnPropertiesWhenAddingFKConstraint() {
      final Column column =
          Column.builder()
              .name("user_id")
              .dataType("uuid")
              .maximumLength(36)
              .constraints(List.of(Constraint.NULLABLE))
              .build();

      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      final var enriched = result.getFirst();
      assertEquals("user_id", enriched.name());
      assertTrue(enriched.isNullable());
      assertEquals("uuid", enriched.dataType());
      assertEquals(36, enriched.maximumLength());
      assertEquals(List.of(Constraint.FK, Constraint.NULLABLE), enriched.constraints());
    }
  }
}
