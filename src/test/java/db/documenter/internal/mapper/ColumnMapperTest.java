package db.documenter.internal.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColumnMapperTest {

  private ColumnMapper columnMapper;

  @BeforeEach
  void setUp() {
    columnMapper = new ColumnMapper();
  }

  @Nested
  class MapUserDefinedTypesTests {

    @Test
    void returnsColumnsUnchangedWhenNoUserDefinedTypes() {
      final Column column1 = Column.builder().name("id").dataType("uuid").build();
      final Column column2 = Column.builder().name("name").dataType("varchar").build();

      final List<Column> result =
          columnMapper.mapUserDefinedTypes(List.of(column1, column2), List.of());

      assertEquals(2, result.size());
      assertEquals("uuid", result.get(0).dataType());
      assertEquals("varchar", result.get(1).dataType());
    }

    @Test
    void mapsUserDefinedTypeToEnumType() {
      final DbEnum dbEnum = DbEnum.builder().columnName("status").enumName("order_status").build();

      final Column column = Column.builder().name("status").dataType("USER-DEFINED").build();

      final List<Column> result =
          columnMapper.mapUserDefinedTypes(List.of(column), List.of(dbEnum));

      assertEquals(1, result.size());
      assertEquals("order_status", result.get(0).dataType());
      assertEquals("status", result.get(0).name());
    }

    @Test
    void keepsUserDefinedTypeWhenNoMatchingEnum() {
      final DbEnum dbEnum =
          DbEnum.builder().columnName("other_column").enumName("other_enum").build();

      final Column column = Column.builder().name("status").dataType("USER-DEFINED").build();

      final List<Column> result =
          columnMapper.mapUserDefinedTypes(List.of(column), List.of(dbEnum));

      assertEquals(1, result.size());
      assertEquals("USER-DEFINED", result.get(0).dataType());
    }

    @Test
    void preservesOtherColumnPropertiesWhenMappingTypes() {
      final DbEnum dbEnum = DbEnum.builder().columnName("status").enumName("order_status").build();

      final Column column =
          Column.builder()
              .name("status")
              .ordinalPosition(5)
              .dataType("USER-DEFINED")
              .maximumLength(50)
              .constraints(List.of(Constraint.DEFAULT, Constraint.NULLABLE))
              .build();

      final List<Column> result =
          columnMapper.mapUserDefinedTypes(List.of(column), List.of(dbEnum));

      assertEquals(1, result.size());
      final var mapped = result.get(0);
      assertEquals("status", mapped.name());
      assertEquals(5, mapped.ordinalPosition());
      assertTrue(mapped.isNullable());
      assertEquals("order_status", mapped.dataType());
      assertEquals(50, mapped.maximumLength());
      assertEquals(List.of(Constraint.DEFAULT, Constraint.NULLABLE), mapped.constraints());
    }
  }

  @Nested
  class EnrichWithForeignKeyConstraintsTests {

    @Test
    void returnsColumnsUnchangedWhenNoForeignKeys() {
      final Column column1 = Column.builder().name("id").dataType("uuid").build();
      final Column column2 = Column.builder().name("name").dataType("varchar").build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column1, column2), List.of());

      assertEquals(2, result.size());
      assertEquals("id", result.get(0).name());
      assertEquals("name", result.get(1).name());
      assertTrue(result.get(0).constraints() == null || result.get(0).constraints().isEmpty());
      assertTrue(result.get(1).constraints() == null || result.get(1).constraints().isEmpty());
    }

    @Test
    void addsFKConstraintToForeignKeyColumn() {
      final Column column = Column.builder().name("user_id").dataType("uuid").build();

      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      assertEquals("user_id", result.get(0).name());
      assertEquals(1, result.get(0).constraints().size());
      assertEquals(Constraint.FK, result.get(0).constraints().get(0));
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
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      assertEquals(3, result.get(0).constraints().size());
      assertEquals(Constraint.FK, result.get(0).constraints().get(0));
      assertEquals(Constraint.UNIQUE, result.get(0).constraints().get(1));
      assertEquals(Constraint.DEFAULT, result.get(0).constraints().get(2));
    }

    @Test
    void handlesMultipleForeignKeyColumns() {
      final Column column1 = Column.builder().name("user_id").dataType("uuid").build();
      final Column column2 = Column.builder().name("product_id").dataType("bigint").build();
      final Column column3 = Column.builder().name("name").dataType("varchar").build();

      final ForeignKey foreignKey1 =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .build();

      final ForeignKey foreignKey2 =
          ForeignKey.builder()
              .name("fk_product")
              .sourceTable("orders")
              .sourceColumn("product_id")
              .targetTable("products")
              .targetColumn("id")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(
              List.of(column1, column2, column3), List.of(foreignKey1, foreignKey2));

      assertEquals(3, result.size());

      // user_id should have FK
      assertEquals("user_id", result.get(0).name());
      assertEquals(1, result.get(0).constraints().size());
      assertEquals(Constraint.FK, result.get(0).constraints().get(0));

      // product_id should have FK
      assertEquals("product_id", result.get(1).name());
      assertEquals(1, result.get(1).constraints().size());
      assertEquals(Constraint.FK, result.get(1).constraints().get(0));

      // name should not have FK
      assertEquals("name", result.get(2).name());
      assertTrue(result.get(2).constraints() == null || result.get(2).constraints().isEmpty());
    }

    @Test
    void isCaseInsensitiveWhenMatchingColumnNames() {
      final Column column = Column.builder().name("USER_ID").dataType("uuid").build();

      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      assertEquals("USER_ID", result.get(0).name());
      assertFalse(result.get(0).constraints().isEmpty());
      assertEquals(Constraint.FK, result.get(0).constraints().get(0));
    }

    @Test
    void preservesOtherColumnPropertiesWhenAddingFKConstraint() {
      final Column column =
          Column.builder()
              .name("user_id")
              .ordinalPosition(3)
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
              .build();

      final List<Column> result =
          columnMapper.enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));

      assertEquals(1, result.size());
      final var enriched = result.get(0);
      assertEquals("user_id", enriched.name());
      assertEquals(3, enriched.ordinalPosition());
      assertTrue(enriched.isNullable());
      assertEquals("uuid", enriched.dataType());
      assertEquals(36, enriched.maximumLength());
      assertEquals(List.of(Constraint.FK, Constraint.NULLABLE), enriched.constraints());
    }
  }
}
