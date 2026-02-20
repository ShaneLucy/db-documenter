package db.documenter.internal.renderer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityRendererTest {

  @Mock private EntityLineFormatter lineFormatter;

  private EntityRenderer entityRenderer;

  @BeforeEach
  void setUp() {
    entityRenderer = new EntityRenderer(lineFormatter);
  }

  @Nested
  class PartitionedTableTests {

    @Test
    void itRendersPartitionedTableWithStereotype() {
      final var table =
          Table.builder()
              .name("daily_project_stats")
              .columns(List.of())
              .foreignKeys(List.of())
              .partitionStrategy("RANGE (stat_date)")
              .build();

      final var result = entityRenderer.render(table);

      assertTrue(
          result.contains("entity \"daily_project_stats\" <<partitioned: RANGE (stat_date)>> {"),
          "Partitioned table header must include <<partitioned: RANGE (stat_date)>> stereotype");
    }

    @Test
    void itRendersPartitionNamesComment() {
      final var table =
          Table.builder()
              .name("daily_project_stats")
              .columns(List.of())
              .foreignKeys(List.of())
              .partitionStrategy("RANGE (stat_date)")
              .partitionNames(List.of("daily_project_stats_2024_q3", "daily_project_stats_2024_q4"))
              .build();

      final var result = entityRenderer.render(table);

      assertTrue(
          result.contains(
              "\t' Partitions: daily_project_stats_2024_q3, daily_project_stats_2024_q4"),
          "Partition names comment must be appended after closing brace");
    }

    @Test
    void itRendersPartitionedTableWithColumnsAndPkAndPartitions() {
      final var idColumn =
          Column.builder()
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var statDateColumn =
          Column.builder()
              .name("stat_date")
              .dataType("date")
              .maximumLength(0)
              .constraints(List.of(Constraint.NULLABLE))
              .build();
      final var primaryKey =
          PrimaryKey.builder()
              .constraintName("pk_daily_project_stats")
              .columnNames(List.of("id"))
              .build();
      final var table =
          Table.builder()
              .name("daily_project_stats")
              .columns(List.of(idColumn, statDateColumn))
              .primaryKey(primaryKey)
              .foreignKeys(List.of())
              .partitionStrategy("RANGE (stat_date)")
              .partitionNames(List.of("daily_project_stats_2024_q3", "daily_project_stats_2024_q4"))
              .build();

      when(lineFormatter.format(any(Table.class), any(Column.class), isNull()))
          .thenReturn("id: uuid")
          .thenReturn("stat_date: date <<NULLABLE>>");

      final var result = entityRenderer.render(table);

      assertTrue(
          result.contains("entity \"daily_project_stats\" <<partitioned: RANGE (stat_date)>> {"),
          "Header must include partitioned stereotype");
      assertTrue(result.contains("\t\tid: uuid"), "PK column must appear before separator");
      assertTrue(
          result.contains("\t\t--\n"), "Separator must appear between PK and non-PK columns");
      assertTrue(
          result.contains("\t\tstat_date: date <<NULLABLE>>"),
          "Non-PK column must appear after separator");
      assertTrue(result.contains("\t}\n"), "Closing brace must be present");
      assertTrue(
          result.contains(
              "\t' Partitions: daily_project_stats_2024_q3, daily_project_stats_2024_q4"),
          "Partition names comment must appear after closing brace");

      final int closingBraceIndex = result.indexOf("\t}\n");
      final int partitionsCommentIndex = result.indexOf("\t' Partitions:");
      assertTrue(
          closingBraceIndex < partitionsCommentIndex,
          "Partition comment must appear after the closing brace");
    }

    @Test
    void itDoesNotRenderPartitionCommentWhenPartitionNamesIsEmpty() {
      final var table =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();

      final var result = entityRenderer.render(table);

      assertTrue(
          result.contains("entity \"users\" {"),
          "Regular table header must use plain entity format without stereotype");
      assertFalse(
          result.contains("<<partitioned:"),
          "Non-partitioned table must not include <<partitioned:>> stereotype");
      assertFalse(
          result.contains("' Partitions:"),
          "Output must not contain a partition comment when partitionNames is empty");
    }
  }

  @Nested
  class RenderNonPartitionedTableTests {

    @Test
    void itRendersTableWithNoColumnsAndNoPrimaryKey() {
      final var table =
          Table.builder().name("orders").columns(List.of()).foreignKeys(List.of()).build();

      final var result = entityRenderer.render(table);

      final var expected =
          """
          \tentity "orders" {
          \t}
          """;

      assertEquals(expected, result);
    }

    @Test
    void itRendersPrimaryKeyColumnAboveSeparatorAndNonPkColumnBelow() {
      final var idColumn =
          Column.builder()
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var nameColumn =
          Column.builder()
              .name("name")
              .dataType("varchar")
              .maximumLength(100)
              .constraints(List.of())
              .build();
      final var primaryKey =
          PrimaryKey.builder().constraintName("pk_orders").columnNames(List.of("id")).build();
      final var table =
          Table.builder()
              .name("orders")
              .columns(List.of(idColumn, nameColumn))
              .primaryKey(primaryKey)
              .foreignKeys(List.of())
              .build();

      when(lineFormatter.format(any(Table.class), any(Column.class), isNull()))
          .thenReturn("id: uuid")
          .thenReturn("name: varchar(100)");

      final var result = entityRenderer.render(table);

      final int idIndex = result.indexOf("\t\tid: uuid");
      final int separatorIndex = result.indexOf("\t\t--\n");
      final int nameIndex = result.indexOf("\t\tname: varchar(100)");

      assertTrue(idIndex < separatorIndex, "PK column must appear before the -- separator");
      assertTrue(separatorIndex < nameIndex, "Non-PK column must appear after the -- separator");
    }

    @Test
    void itDoesNotRenderSeparatorWhenAllColumnsAreInPrimaryKey() {
      final var idColumn =
          Column.builder()
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var primaryKey =
          PrimaryKey.builder().constraintName("pk_orders").columnNames(List.of("id")).build();
      final var table =
          Table.builder()
              .name("orders")
              .columns(List.of(idColumn))
              .primaryKey(primaryKey)
              .foreignKeys(List.of())
              .build();

      when(lineFormatter.format(any(Table.class), any(Column.class), isNull()))
          .thenReturn("id: uuid");

      final var result = entityRenderer.render(table);

      assertFalse(
          result.contains("--"), "Separator must not appear when there are no non-PK columns");
    }

    @Test
    void itDoesNotRenderSeparatorWhenNoPrimaryKeyIsSet() {
      final var nameColumn =
          Column.builder()
              .name("name")
              .dataType("varchar")
              .maximumLength(100)
              .constraints(List.of())
              .build();
      final var table =
          Table.builder().name("tags").columns(List.of(nameColumn)).foreignKeys(List.of()).build();

      when(lineFormatter.format(any(Table.class), any(Column.class), isNull()))
          .thenReturn("name: varchar(100)");

      final var result = entityRenderer.render(table);

      assertFalse(result.contains("--"), "Separator must not appear when there is no primary key");
    }
  }
}
