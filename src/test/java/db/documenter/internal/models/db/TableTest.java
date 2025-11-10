package db.documenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableTest {

  private Table baseTable;
  private List<Column> columns;
  private PrimaryKey primaryKey;
  private List<ForeignKey> foreignKeys;

  @BeforeEach
  void setUp() {
    columns =
        List.of(
            Column.builder()
                .name("id")
                .ordinalPosition(1)
                .isNullable(false)
                .dataType("int")
                .maximumLength(10)
                .build(),
            Column.builder()
                .name("name")
                .ordinalPosition(2)
                .isNullable(true)
                .dataType("varchar")
                .maximumLength(50)
                .build());
    primaryKey = PrimaryKey.builder().constraintName("pk_test").columnNames(List.of("id")).build();
    foreignKeys =
        List.of(
            ForeignKey.builder()
                .name("fk_test")
                .sourceTable("table1")
                .sourceColumn("id")
                .targetTable("table2")
                .targetColumn("id")
                .build());
    baseTable = Table.builder().schema("public").name("my_table").type("BASE TABLE").build();
  }

  @Test
  void itCombinesTableColumnsPrimaryAndForeignKeysCorrectly() {
    final Table result =
        Table.combineTableColumnsPrimaryAndForeignKeys(baseTable, columns, primaryKey, foreignKeys);

    assertEquals(baseTable.schema(), result.schema());
    assertEquals(baseTable.name(), result.name());
    assertEquals(baseTable.type(), result.type());

    // Columns, PK, and FKs are preserved
    assertSame(columns, result.columns());
    assertSame(primaryKey, result.primaryKey());
    assertSame(foreignKeys, result.foreignKeys());
  }
}
