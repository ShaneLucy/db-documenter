package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.catalog.PrimaryKey;
import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.UniqueKey;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ColumnRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ConstraintRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TableRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TypeRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.UniqueIndexRow;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the per-table rules of {@link PgTableAssembler}: which keys are usable and how
 * constraint and index keys combine. Joining across tables is covered by {@code
 * PgCatalogAssemblerTest}.
 */
class PgTableAssemblerTest {

  private static final long OID = 6L;
  private static final TableRow TABLE = new TableRow(OID, "s6", "t", 2);
  private static final TypeRow INTEGER = new TypeRow("integer", "pg_catalog", "int4", null, null);
  private static final List<ColumnRow> COLUMNS =
      List.of(new ColumnRow(OID, "a", false, INTEGER), new ColumnRow(OID, "b", true, INTEGER));

  private LogCapture logCapture;

  @BeforeEach
  void setUp() {
    logCapture = LogCapture.attach(PgCatalogAssembler.class.getName());
  }

  @AfterEach
  void tearDown() {
    logCapture.detach();
  }

  private static ConstraintRow constraint(
      final String name, final ConstraintType type, final String... columns) {
    return ConstraintRow.builder()
        .tableOid(OID)
        .name(name)
        .type(type)
        .columns(List.of(columns))
        .build();
  }

  @Nested
  class KeyUsabilityTests {

    @Test
    void itSkipsConstraintListingColumnTwice() {
      final var table =
          PgTableAssembler.toTable(
              TABLE,
              COLUMNS,
              List.of(constraint("t_pkey", ConstraintType.PRIMARY_KEY, "a", "a")),
              List.of());

      assertTrue(table.primaryKey().isEmpty());
      assertEquals(
          List.of("Skipped key s6.t t_pkey because it lists a column more than once"),
          logCapture.warnings());
    }

    @Test
    void itSkipsIndexListingColumnTwice() {
      final var table =
          PgTableAssembler.toTable(
              TABLE,
              COLUMNS,
              List.of(),
              List.of(new UniqueIndexRow(OID, "t_aa", List.of("a", "a"))));

      assertEquals(List.of(), table.uniqueKeys());
      assertEquals(
          List.of("Skipped key s6.t t_aa because it lists a column more than once"),
          logCapture.warnings());
    }

    @Test
    void itReportsMissingColumnBeforeDuplicateColumn() {
      PgTableAssembler.toTable(
          TABLE, COLUMNS, List.of(), List.of(new UniqueIndexRow(OID, "t_gg", List.of("g", "g"))));

      assertEquals(
          List.of(
              "Dropped key s6.t t_gg because it names a column that was not read; the catalog"
                  + " probably changed during the read"),
          logCapture.warnings());
    }

    @Test
    void itKeepsKeysWhoseColumnsAreKnownAndDistinct() {
      final var table =
          PgTableAssembler.toTable(
              TABLE,
              COLUMNS,
              List.of(constraint("t_pkey", ConstraintType.PRIMARY_KEY, "b", "a")),
              List.of(new UniqueIndexRow(OID, "t_ab", List.of("a", "b"))));

      assertEquals(
          PrimaryKey.builder().name("t_pkey").columns(List.of("b", "a")).build(),
          table.primaryKey().orElseThrow());
      assertEquals(
          List.of(UniqueKey.builder().name("t_ab").columns(List.of("a", "b")).build()),
          table.uniqueKeys());
      assertEquals(List.of(), logCapture.warnings());
    }
  }

  @Nested
  class ToTableTests {

    @Test
    void itQualifiesTableNameWithItsSchema() {
      final var table = PgTableAssembler.toTable(TABLE, COLUMNS, List.of(), List.of());

      assertEquals(QualifiedName.of("s6", "t"), table.name());
    }

    @Test
    void itCombinesUniqueConstraintsAndIndexesIntoUniqueKeys() {
      final var table =
          PgTableAssembler.toTable(
              TABLE,
              COLUMNS,
              List.of(constraint("t_b_key", ConstraintType.UNIQUE, "b")),
              List.of(new UniqueIndexRow(OID, "t_a_idx", List.of("a"))));

      assertEquals(
          List.of(
              UniqueKey.builder().name("t_a_idx").columns(List.of("a")).build(),
              UniqueKey.builder().name("t_b_key").columns(List.of("b")).build()),
          table.uniqueKeys());
    }

    @Test
    void itDoesNotTreatUniqueOrForeignKeyAsPrimaryKey() {
      final var table =
          PgTableAssembler.toTable(
              TABLE,
              COLUMNS,
              List.of(
                  constraint("t_a_key", ConstraintType.UNIQUE, "a"),
                  ConstraintRow.builder()
                      .tableOid(OID)
                      .name("t_b_fkey")
                      .type(ConstraintType.FOREIGN_KEY)
                      .columns(List.of("b"))
                      .referencedTable(QualifiedName.of("s6", "t"))
                      .referencedColumns(List.of("a"))
                      .build()),
              List.of());

      assertTrue(table.primaryKey().isEmpty());
      assertEquals(1, table.uniqueKeys().size());
      assertEquals(1, table.foreignKeys().size());
    }

    @Test
    void itThrowsExceptionWhenForeignKeyHasNoReferencedTable() {
      final var foreignKey =
          ConstraintRow.builder()
              .tableOid(OID)
              .name("t_b_fkey")
              .type(ConstraintType.FOREIGN_KEY)
              .columns(List.of("b"))
              .referencedColumns(List.of("a"))
              .build();

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> PgTableAssembler.toTable(TABLE, COLUMNS, List.of(foreignKey), List.of()));

      assertEquals("Foreign key t_b_fkey has no referenced table", exception.getMessage());
    }
  }
}
