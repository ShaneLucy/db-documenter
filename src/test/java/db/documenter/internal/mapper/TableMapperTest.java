package db.documenter.internal.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Modernizer is flagging Prefer java.util.Optional.orElseThrow
// but for tests classes this isn't as clean
@SuppressModernizer
class TableMapperTest {

  private TableMapper tableMapper;

  @BeforeEach
  void setUp() {
    tableMapper = new TableMapper();
  }

  @Nested
  class CombineTableComponentsTests {

    @Test
    void combinesTableComponentsIntoTable() {
      final Column column =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final PrimaryKey primaryKey =
          PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();
      final ForeignKey foreignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final Table result =
          tableMapper.combineTableComponents(
              "orders", List.of(column), primaryKey, List.of(foreignKey));

      assertNotNull(result);
      assertEquals("orders", result.name());
      assertEquals(1, result.columns().size());
      assertEquals("id", result.columns().getFirst().name());
      assertTrue(result.primaryKey().isPresent());
      assertEquals(List.of("id"), result.primaryKey().map(PrimaryKey::columnNames).orElseThrow());
      assertEquals(1, result.foreignKeys().size());
      assertEquals("fk_user", result.foreignKeys().getFirst().name());
    }

    @Test
    void handlesTableWithNoPrimaryKey() {
      final Column column =
          Column.builder().name("tag_id").dataType("varchar").constraints(List.of()).build();

      final Table result =
          tableMapper.combineTableComponents("tag_log", List.of(column), null, List.of());

      assertNotNull(result);
      assertEquals("tag_log", result.name());
      assertEquals(1, result.columns().size());
      assertTrue(result.primaryKey().isEmpty());
      assertTrue(result.foreignKeys().isEmpty());
    }

    @Test
    void handlesTableWithNoForeignKeys() {
      final Column column =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final PrimaryKey primaryKey =
          PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();

      final Table result =
          tableMapper.combineTableComponents("users", List.of(column), primaryKey, List.of());

      assertNotNull(result);
      assertEquals("users", result.name());
      assertEquals(1, result.columns().size());
      assertNotNull(result.primaryKey());
      assertTrue(result.foreignKeys().isEmpty());
    }

    @Test
    void handlesTableWithMultipleColumns() {
      final Column column1 =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final Column column2 =
          Column.builder().name("name").dataType("varchar").constraints(List.of()).build();
      final Column column3 =
          Column.builder().name("email").dataType("varchar").constraints(List.of()).build();

      final PrimaryKey primaryKey =
          PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();

      final Table result =
          tableMapper.combineTableComponents(
              "users", List.of(column1, column2, column3), primaryKey, List.of());

      assertEquals(3, result.columns().size());
      assertEquals("id", result.columns().getFirst().name());
      assertEquals("name", result.columns().get(1).name());
      assertEquals("email", result.columns().get(2).name());
    }

    @Test
    void handlesTableWithMultipleForeignKeys() {
      final Column column =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final PrimaryKey primaryKey =
          PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();

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

      final Table result =
          tableMapper.combineTableComponents(
              "orders", List.of(column), primaryKey, List.of(foreignKey1, foreignKey2));

      assertEquals(2, result.foreignKeys().size());
      assertEquals("fk_user", result.foreignKeys().getFirst().name());
      assertEquals("fk_product", result.foreignKeys().get(1).name());
    }

    @Test
    void handlesTableWithCompositePrimaryKey() {
      final Column column1 =
          Column.builder().name("user_id").dataType("uuid").constraints(List.of()).build();
      final Column column2 =
          Column.builder().name("role_id").dataType("smallint").constraints(List.of()).build();

      final PrimaryKey primaryKey =
          PrimaryKey.builder()
              .constraintName("pk_test")
              .columnNames(List.of("user_id", "role_id"))
              .build();

      final Table result =
          tableMapper.combineTableComponents(
              "user_role", List.of(column1, column2), primaryKey, List.of());

      assertTrue(result.primaryKey().isPresent());
      assertEquals(2, result.primaryKey().get().columnNames().size());
      assertEquals("user_id", result.primaryKey().get().columnNames().getFirst());
      assertEquals("role_id", result.primaryKey().get().columnNames().get(1));
    }

    @Test
    void handlesEmptyColumnsList() {
      final PrimaryKey primaryKey =
          PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();

      final Table result =
          tableMapper.combineTableComponents("test_table", List.of(), primaryKey, List.of());

      assertNotNull(result);
      assertEquals("test_table", result.name());
      assertTrue(result.columns().isEmpty());
    }
  }
}
