package dev.shanelucy.dbdocumenter.internal.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.internal.formatter.impl.multiplicity.CardinalityFormatter;
import dev.shanelucy.dbdocumenter.internal.models.db.Column;
import dev.shanelucy.dbdocumenter.internal.models.db.Constraint;
import dev.shanelucy.dbdocumenter.internal.models.db.ForeignKey;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
          Column.builder()
              .isNullable(false)
              .name("id")
              .dataType("varchar")
              .constraints(List.of())
              .build();

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
              .isNullable(false)
              .build();

      final Column column =
          Column.builder()
              .isNullable(true)
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.UNIQUE))
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
              .isNullable(false)
              .build();

      final Column column =
          Column.builder()
              .isNullable(false)
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of())
              .build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

      assertEquals(1, result.size());
      assertFalse(result.getFirst().isNullable());
    }

    @Test
    void itDoesNotMatchSourceColumnWhenOnlyTheCaseDiffers() {
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();
      final Column column =
          Column.builder()
              .isNullable(true)
              .name("USER_ID")
              .dataType("uuid")
              .constraints(List.of())
              .build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), List.of(column));

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
              .isNullable(false)
              .build();

      final Column column =
          Column.builder()
              .isNullable(false)
              .name("other_column")
              .dataType("varchar")
              .constraints(List.of())
              .build();

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
              .isNullable(false)
              .build();

      final ForeignKey foreignKey2 =
          ForeignKey.builder()
              .name("fk_product")
              .sourceTable("orders")
              .sourceColumn("product_id")
              .targetTable("products")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final Column column1 =
          Column.builder()
              .isNullable(true)
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.UNIQUE))
              .build();
      final Column column2 =
          Column.builder()
              .isNullable(false)
              .name("product_id")
              .dataType("varchar")
              .constraints(List.of())
              .build();

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
              .isNullable(false)
              .build();

      final Column column =
          Column.builder()
              .isNullable(true)
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.UNIQUE))
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
              .isNullable(false)
              .build();

      final Column column1 =
          Column.builder()
              .isNullable(false)
              .name("id")
              .dataType("varchar")
              .constraints(List.of())
              .build();
      final Column column2 =
          Column.builder()
              .isNullable(true)
              .name("user_id")
              .dataType("varchar")
              .constraints(List.of(Constraint.UNIQUE))
              .build();
      final Column column3 =
          Column.builder()
              .isNullable(false)
              .name("product_id")
              .dataType("varchar")
              .constraints(List.of())
              .build();

      final List<ForeignKey> result =
          foreignKeyMapper.enrichWithNullability(
              List.of(foreignKey), List.of(column1, column2, column3));

      assertEquals(1, result.size());
      assertTrue(result.getFirst().isNullable());
    }
  }

  @Nested
  class NullabilityToCardinalityTests {

    @ParameterizedTest
    @CsvSource({"true,orders ||--o{ users", "false,orders ||--|{ users"})
    void itCarriesColumnNullabilityThroughColumnEnrichmentIntoCardinality(
        final boolean isNullable, final String expectedLine) {
      final Column column =
          Column.builder().isNullable(isNullable).name("user_id").dataType("uuid").build();
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .isNullable(false)
              .build();

      final var enrichedColumns =
          new ColumnMapper().enrichWithForeignKeyConstraints(List.of(column), List.of(foreignKey));
      final var enrichedForeignKeys =
          foreignKeyMapper.enrichWithNullability(List.of(foreignKey), enrichedColumns);

      assertEquals(
          expectedLine,
          new CardinalityFormatter()
              .format(enrichedForeignKeys.getFirst(), "public", "orders -- users"));
    }
  }
}
