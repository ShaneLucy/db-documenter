package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import db.documenter.internal.mapper.ColumnMapper;
import db.documenter.internal.mapper.ColumnMappingContext;
import db.documenter.internal.mapper.ForeignKeyMapper;
import db.documenter.internal.mapper.TableMapper;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
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
class TableBuilderTest {

  @Mock private QueryRunner queryRunner;
  @Mock private ColumnMapper columnMapper;
  @Mock private ForeignKeyMapper foreignKeyMapper;
  @Mock private TableMapper tableMapper;

  private TableBuilder tableBuilder;

  @BeforeEach
  void setUp() {
    reset(queryRunner, columnMapper, foreignKeyMapper, tableMapper);
    tableBuilder = new TableBuilder(columnMapper, foreignKeyMapper, tableMapper);
  }

  @Nested
  class BuildTablesTests {

    @Test
    void returnsEmptyListWhenNoTablesExist() throws SQLException {
      when(queryRunner.getTableInfo("test_schema")).thenReturn(List.of());

      final List<Table> result =
          tableBuilder.buildTables(queryRunner, "test_schema", List.of(), Map.of(), Map.of());

      assertTrue(result.isEmpty());
    }

    @Test
    void buildsTablesWithColumnsAndKeys() throws SQLException {
      final Table table1 =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();
      final Table table2 =
          Table.builder().name("orders").columns(List.of()).foreignKeys(List.of()).build();

      final Column rawColumn1 =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final Column rawColumn2 =
          Column.builder().name("name").dataType("varchar").constraints(List.of()).build();

      final Column mappedColumn1 =
          Column.builder().name("id").dataType("uuid").constraints(List.of()).build();
      final Column mappedColumn2 =
          Column.builder().name("name").dataType("varchar").constraints(List.of()).build();

      final PrimaryKey primaryKey =
          PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();

      final ForeignKey rawForeignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .referencedSchema("public")
              .build();

      final ForeignKey enrichedForeignKey =
          ForeignKey.builder()
              .name("fk_user")
              .sourceTable("orders")
              .sourceColumn("user_id")
              .targetTable("users")
              .targetColumn("id")
              .isNullable(false)
              .referencedSchema("public")
              .build();

      final Table builtTable1 =
          Table.builder()
              .name("users")
              .columns(List.of(mappedColumn1))
              .primaryKey(primaryKey)
              .foreignKeys(List.of())
              .build();

      final Table builtTable2 =
          Table.builder()
              .name("orders")
              .columns(List.of(mappedColumn2))
              .primaryKey(primaryKey)
              .foreignKeys(List.of(enrichedForeignKey))
              .build();

      final Map<EnumKey, DbEnum> enumsByKey = Map.of();
      final Map<ColumnKey, UdtReference> columnUdtMappings = Map.of();

      when(queryRunner.getTableInfo("test_schema")).thenReturn(List.of(table1, table2));

      when(queryRunner.getColumnInfo("test_schema", table1)).thenReturn(List.of(rawColumn1));
      when(columnMapper.mapUserDefinedTypes(
              eq(List.of(rawColumn1)), any(ColumnMappingContext.class)))
          .thenReturn(List.of(mappedColumn1));
      when(queryRunner.getPrimaryKeyInfo("test_schema", table1)).thenReturn(primaryKey);
      when(queryRunner.getForeignKeyInfo("test_schema", table1)).thenReturn(List.of());
      when(foreignKeyMapper.enrichWithNullability(List.of(), List.of(mappedColumn1)))
          .thenReturn(List.of());
      when(columnMapper.enrichWithForeignKeyConstraints(List.of(mappedColumn1), List.of()))
          .thenReturn(List.of(mappedColumn1));
      when(tableMapper.combineTableComponents(
              "users", List.of(mappedColumn1), primaryKey, List.of()))
          .thenReturn(builtTable1);

      when(queryRunner.getColumnInfo("test_schema", table2)).thenReturn(List.of(rawColumn2));
      when(columnMapper.mapUserDefinedTypes(
              eq(List.of(rawColumn2)), any(ColumnMappingContext.class)))
          .thenReturn(List.of(mappedColumn2));
      when(queryRunner.getPrimaryKeyInfo("test_schema", table2)).thenReturn(primaryKey);
      when(queryRunner.getForeignKeyInfo("test_schema", table2)).thenReturn(List.of(rawForeignKey));
      when(foreignKeyMapper.enrichWithNullability(List.of(rawForeignKey), List.of(mappedColumn2)))
          .thenReturn(List.of(enrichedForeignKey));
      when(columnMapper.enrichWithForeignKeyConstraints(
              List.of(mappedColumn2), List.of(rawForeignKey)))
          .thenReturn(List.of(mappedColumn2));
      when(tableMapper.combineTableComponents(
              "orders", List.of(mappedColumn2), primaryKey, List.of(enrichedForeignKey)))
          .thenReturn(builtTable2);

      final List<Table> result =
          tableBuilder.buildTables(
              queryRunner, "test_schema", List.of(), enumsByKey, columnUdtMappings);

      assertEquals(2, result.size());
      assertEquals("users", result.getFirst().name());
      assertEquals("orders", result.get(1).name());
    }

    @Test
    void passesEnumsByKeyAndColumnUdtMappingsToColumnMapper() throws SQLException {
      final Table table =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();
      final DbEnum dbEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of())
              .build();

      final Column rawColumn =
          Column.builder().name("status").dataType("USER-DEFINED").constraints(List.of()).build();
      final Column mappedColumn =
          Column.builder().name("status").dataType("status").constraints(List.of()).build();

      final PrimaryKey primaryKey =
          PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();

      final Table builtTable =
          Table.builder()
              .name("users")
              .columns(List.of(mappedColumn))
              .primaryKey(primaryKey)
              .foreignKeys(List.of())
              .build();

      final var enumKey = new EnumKey("test_schema", "status");
      final Map<EnumKey, DbEnum> enumsByKey = Map.of(enumKey, dbEnum);
      final var columnKey = new ColumnKey("users", "status");
      final var udtReference = new UdtReference("test_schema", "status");
      final Map<ColumnKey, UdtReference> columnUdtMappings = Map.of(columnKey, udtReference);

      when(queryRunner.getTableInfo("test_schema")).thenReturn(List.of(table));
      when(queryRunner.getColumnInfo("test_schema", table)).thenReturn(List.of(rawColumn));
      when(columnMapper.mapUserDefinedTypes(
              eq(List.of(rawColumn)), any(ColumnMappingContext.class)))
          .thenReturn(List.of(mappedColumn));
      when(queryRunner.getPrimaryKeyInfo("test_schema", table)).thenReturn(primaryKey);
      when(queryRunner.getForeignKeyInfo("test_schema", table)).thenReturn(List.of());
      when(foreignKeyMapper.enrichWithNullability(List.of(), List.of(mappedColumn)))
          .thenReturn(List.of());
      when(columnMapper.enrichWithForeignKeyConstraints(List.of(mappedColumn), List.of()))
          .thenReturn(List.of(mappedColumn));
      when(tableMapper.combineTableComponents(
              "users", List.of(mappedColumn), primaryKey, List.of()))
          .thenReturn(builtTable);

      final List<Table> result =
          tableBuilder.buildTables(
              queryRunner, "test_schema", List.of(dbEnum), enumsByKey, columnUdtMappings);

      verify(columnMapper)
          .mapUserDefinedTypes(eq(List.of(rawColumn)), any(ColumnMappingContext.class));
      assertEquals(1, result.size());
    }

    @Test
    void propagatesSQLExceptionFromGetTableInfo() throws SQLException {
      when(queryRunner.getTableInfo("test_schema"))
          .thenThrow(new SQLException("Connection failed"));

      final SQLException exception =
          assertThrows(
              SQLException.class,
              () ->
                  tableBuilder.buildTables(
                      queryRunner, "test_schema", List.of(), Map.of(), Map.of()));

      assertEquals("Connection failed", exception.getMessage());
    }

    @Test
    void propagatesSQLExceptionFromGetColumnInfo() throws SQLException {
      final Table table =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();

      when(queryRunner.getTableInfo("test_schema")).thenReturn(List.of(table));
      when(queryRunner.getColumnInfo("test_schema", table))
          .thenThrow(new SQLException("Failed to fetch columns"));

      final SQLException exception =
          assertThrows(
              SQLException.class,
              () ->
                  tableBuilder.buildTables(
                      queryRunner, "test_schema", List.of(), Map.of(), Map.of()));

      assertEquals("Failed to fetch columns", exception.getMessage());
    }
  }
}
