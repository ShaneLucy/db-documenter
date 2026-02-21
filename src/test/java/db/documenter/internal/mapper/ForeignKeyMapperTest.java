package db.documenter.internal.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.ReferentialAction;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ForeignKeyMapperTest {

  private ForeignKeyMapper foreignKeyMapper;

  @BeforeEach
  void setUp() {
    foreignKeyMapper = new ForeignKeyMapper();
  }

  @Nested
  class EnrichWithNullabilityTests {

    @Test
    void returnsEmptyListWhenNoForeignKeys() {
      final Column column =
          Column.builder().name("id").dataType("varchar").constraints(List.of()).build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(List.of(), List.of(column));

      assertTrue(result.isEmpty());
    }

    @Test
    void enrichesForeignKeyWithNullabilityFromColumn() {
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final Column column =
          Column.builder()
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.NULLABLE))
              .build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      assertEquals(1, result.size());
      assertTrue(result.getFirst().isNullable());
    }

    @Test
    void enrichesForeignKeyWithNonNullableColumn() {
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final Column column =
          Column.builder().name("user_id").dataType("varchar").constraints(List.of()).build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      assertEquals(1, result.size());
      assertFalse(result.getFirst().isNullable());
    }

    @Test
    void preservesForeignKeyWhenColumnNotFound() {
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final Column column =
          Column.builder().name("other_column").dataType("varchar").constraints(List.of()).build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      assertEquals(1, result.size());
      assertFalse(result.getFirst().isNullable());
    }

    @Test
    void handlesMultipleForeignKeys() {
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

      final Column column1 =
          Column.builder()
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.NULLABLE))
              .build();
      final Column column2 =
          Column.builder().name("product_id").dataType("varchar").constraints(List.of()).build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(
              List.of(foreignKey1, foreignKey2), List.of(column1, column2));

      assertEquals(2, result.size());
      assertTrue(result.getFirst().isNullable());
      assertFalse(result.get(1).isNullable());
    }

    @Test
    void preservesOtherForeignKeyPropertiesWhenEnriching() {
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final Column column =
          Column.builder()
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.NULLABLE))
              .build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      assertEquals(1, result.size());
      final var enriched = result.getFirst();
      assertEquals("fk_user", enriched.name());
      assertEquals("orders", enriched.sourceTable());
      assertEquals("user_id", enriched.sourceColumn());
      assertEquals("users", enriched.targetTable());
      assertEquals("id", enriched.targetColumn());
      assertTrue(enriched.isNullable());
      assertEquals(ReferentialAction.NO_ACTION, enriched.onDeleteAction());
      assertEquals(ReferentialAction.NO_ACTION, enriched.onUpdateAction());
    }

    @Test
    void handlesMultipleColumnsAndFindsCorrectMatch() {
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final Column column1 =
          Column.builder().name("id").dataType("varchar").constraints(List.of()).build();
      final Column column2 =
          Column.builder()
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.NULLABLE))
              .build();
      final Column column3 =
          Column.builder().name("product_id").dataType("varchar").constraints(List.of()).build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(
              List.of(foreignKey), List.of(column1, column2, column3));

      assertEquals(1, result.size());
      assertTrue(result.getFirst().isNullable());
    }

    @Test
    void itPreservesOnDeleteActionWhenEnrichingNullability() {
      final var foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.CASCADE)
              .build();

      final var column =
          Column.builder()
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.NULLABLE))
              .build();

      final var result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      assertEquals(ReferentialAction.CASCADE, result.getFirst().onDeleteAction());
    }

    @Test
    void itPreservesOnUpdateActionWhenEnrichingNullability() {
      final var foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onUpdateAction(ReferentialAction.RESTRICT)
              .build();

      final var column =
          Column.builder()
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.NULLABLE))
              .build();

      final var result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      assertEquals(ReferentialAction.RESTRICT, result.getFirst().onUpdateAction());
    }

    @Test
    void itPreservesReferentialActionsWhenColumnNotFound() {
      final var foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .onDeleteAction(ReferentialAction.CASCADE)
              .onUpdateAction(ReferentialAction.SET_NULL)
              .build();

      final var column =
          Column.builder().name("other_column").dataType("varchar").constraints(List.of()).build();

      final var result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      final var preserved = result.getFirst();
      assertEquals(ReferentialAction.CASCADE, preserved.onDeleteAction());
      assertEquals(ReferentialAction.SET_NULL, preserved.onUpdateAction());
    }
  }
}
