package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import dev.shanelucy.dbdocumenter.catalog.Column;
import dev.shanelucy.dbdocumenter.catalog.DataType;
import dev.shanelucy.dbdocumenter.catalog.EnumType;
import dev.shanelucy.dbdocumenter.catalog.ForeignKey;
import dev.shanelucy.dbdocumenter.catalog.PrimaryKey;
import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.ReferentialAction;
import dev.shanelucy.dbdocumenter.catalog.Schema;
import dev.shanelucy.dbdocumenter.catalog.Table;
import dev.shanelucy.dbdocumenter.catalog.UniqueKey;
import dev.shanelucy.dbdocumenter.internal.catalog.CatalogDumpOptions;
import dev.shanelucy.dbdocumenter.internal.catalog.CatalogDumpWriter;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ColumnRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ConstraintRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.EnumRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TableRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TypeRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.UniqueIndexRow;
import java.util.List;
import java.util.logging.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PgCatalogAssemblerTest {

  private static final String PRODUCT_NAME = "PostgreSQL";
  private static final String PRODUCT_VERSION = "18.1";
  private static final long CUSTOMERS_OID = 100L;
  private static final long ORDERS_OID = 200L;
  private static final long UNKNOWN_OID = 999L;
  private static final TypeRow INTEGER = new TypeRow("integer", "pg_catalog", "int4", null, null);
  private static final TypeRow TEXT = new TypeRow("text", "pg_catalog", "text", null, null);

  private LogCapture logCapture;

  @BeforeEach
  void setUp() {
    logCapture = LogCapture.attach(PgCatalogAssembler.class.getName());
  }

  @AfterEach
  void tearDown() {
    logCapture.detach();
  }

  private static CatalogSnapshot assemble(final PgCatalogRows rows) {
    return PgCatalogAssembler.assemble(PRODUCT_NAME, PRODUCT_VERSION, rows);
  }

  private static Table onlyTable(final CatalogSnapshot snapshot) {
    return snapshot.schemas().getFirst().tables().getFirst();
  }

  private static ConstraintRow.Builder constraint(
      final long tableOid, final String name, final ConstraintType type, final String... columns) {
    return ConstraintRow.builder()
        .tableOid(tableOid)
        .name(name)
        .type(type)
        .columns(List.of(columns));
  }

  private static TableRow customersTable(final int columnCount) {
    return new TableRow(CUSTOMERS_OID, "core", "customers", columnCount);
  }

  /** A customers table with two columns and nothing else, the base most tests extend. */
  private static PgCatalogRows.Builder customersRows() {
    return PgCatalogRows.builder()
        .schemaNames(List.of("core"))
        .tables(List.of(customersTable(2)))
        .columns(
            List.of(
                new ColumnRow(CUSTOMERS_OID, "id", false, INTEGER),
                new ColumnRow(CUSTOMERS_OID, "email", true, TEXT)));
  }

  private List<String> warnings() {
    return logCapture.warnings();
  }

  @Nested
  class AssembleTests {

    @Test
    void itJoinsRowsIntoSnapshot() {
      final var rows =
          PgCatalogRows.builder()
              .schemaNames(List.of("shop", "core"))
              .tables(
                  List.of(
                      new TableRow(ORDERS_OID, "shop", "orders", 2),
                      new TableRow(CUSTOMERS_OID, "core", "customers", 2)))
              .columns(
                  List.of(
                      new ColumnRow(ORDERS_OID, "order_id", false, INTEGER),
                      new ColumnRow(CUSTOMERS_OID, "id", false, INTEGER),
                      new ColumnRow(ORDERS_OID, "customer_id", true, INTEGER),
                      new ColumnRow(
                          CUSTOMERS_OID,
                          "mood",
                          true,
                          new TypeRow("\"Mood\"", "core", "Mood", null, null))))
              .constraints(
                  List.of(
                      constraint(ORDERS_OID, "orders_pkey", ConstraintType.PRIMARY_KEY, "order_id")
                          .build(),
                      constraint(
                              ORDERS_OID,
                              "orders_customer_id_fkey",
                              ConstraintType.FOREIGN_KEY,
                              "customer_id")
                          .referencedTable(QualifiedName.of("core", "customers"))
                          .referencedColumns(List.of("id"))
                          .onDelete('c')
                          .onUpdate('r')
                          .build(),
                      constraint(CUSTOMERS_OID, "customers_pkey", ConstraintType.PRIMARY_KEY, "id")
                          .build()))
              .uniqueIndexes(
                  List.of(new UniqueIndexRow(CUSTOMERS_OID, "customers_mood_idx", List.of("mood"))))
              .enums(List.of(new EnumRow("core", "Mood", List.of("happy", "sad"))))
              .build();

      final var snapshot = assemble(rows);

      assertEquals(PRODUCT_NAME, snapshot.productName());
      assertEquals(PRODUCT_VERSION, snapshot.productVersion());
      assertEquals(
          """
          schema core
          table core.customers
          column core.customers.id type="integer" nullable=false
          column core.customers.mood type="Mood" nullable=true user_type=core."Mood"
          pk core.customers(id) name=customers_pkey
          unique core.customers(mood) name=customers_mood_idx
          enum core."Mood" values=("happy", "sad")
          schema shop
          table shop.orders
          column shop.orders.order_id type="integer" nullable=false
          column shop.orders.customer_id type="integer" nullable=true
          pk shop.orders(order_id) name=orders_pkey
          fk shop.orders(customer_id) -> core.customers(id) on_delete=cascade on_update=restrict \
          name=orders_customer_id_fkey
          """,
          CatalogDumpWriter.write(snapshot, CatalogDumpOptions.verbatim()));
      assertEquals(List.of(), warnings());
    }

    @Test
    void itKeepsColumnsInRowOrder() {
      final var rows =
          customersRows()
              .tables(List.of(customersTable(3)))
              .columns(
                  List.of(
                      new ColumnRow(CUSTOMERS_OID, "z", true, TEXT),
                      new ColumnRow(CUSTOMERS_OID, "a", true, TEXT),
                      new ColumnRow(CUSTOMERS_OID, "m", true, TEXT)))
              .build();

      final var columns = onlyTable(assemble(rows)).columns().stream().map(Column::name).toList();

      assertEquals(List.of("z", "a", "m"), columns);
    }

    @Test
    void itMergesUniqueKeysFromConstraintsAndIndexes() {
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(
                              CUSTOMERS_OID, "customers_email_key", ConstraintType.UNIQUE, "email")
                          .build()))
              .uniqueIndexes(
                  List.of(
                      new UniqueIndexRow(
                          CUSTOMERS_OID, "customers_id_email_idx", List.of("id", "email"))))
              .build();

      final var uniqueKeys = onlyTable(assemble(rows)).uniqueKeys();

      assertEquals(
          List.of(
              UniqueKey.builder().name("customers_email_key").columns(List.of("email")).build(),
              UniqueKey.builder()
                  .name("customers_id_email_idx")
                  .columns(List.of("id", "email"))
                  .build()),
          uniqueKeys);
    }

    @Test
    void itKeepsForeignKeyWhoseTargetIsOutOfScope() {
      final var target = QualifiedName.of("external_ref", "currencies");
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(
                              CUSTOMERS_OID,
                              "customers_email_fkey",
                              ConstraintType.FOREIGN_KEY,
                              "email")
                          .referencedTable(target)
                          .referencedColumns(List.of("code"))
                          .build()))
              .build();

      final var snapshot = assemble(rows);

      assertEquals(
          List.of(
              ForeignKey.builder()
                  .name("customers_email_fkey")
                  .columns(List.of("email"))
                  .referencedTable(target)
                  .referencedColumns(List.of("code"))
                  .onDelete(ReferentialAction.NO_ACTION)
                  .onUpdate(ReferentialAction.NO_ACTION)
                  .build()),
          onlyTable(snapshot).foreignKeys());
      assertEquals(List.of("core"), snapshot.schemas().stream().map(Schema::name).toList());
      assertEquals(List.of(), warnings());
    }

    @Test
    void itKeepsTwoForeignKeysThatDifferOnlyByName() {
      final var target = QualifiedName.of("core", "customers");
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(CUSTOMERS_OID, "fk_one", ConstraintType.FOREIGN_KEY, "id")
                          .referencedTable(target)
                          .referencedColumns(List.of("id"))
                          .build(),
                      constraint(CUSTOMERS_OID, "fk_two", ConstraintType.FOREIGN_KEY, "id")
                          .referencedTable(target)
                          .referencedColumns(List.of("id"))
                          .build()))
              .build();

      assertEquals(2, onlyTable(assemble(rows)).foreignKeys().size());
    }

    @Test
    void itBuildsTableWithNoColumns() {
      final var rows =
          customersRows().tables(List.of(customersTable(0))).columns(List.of()).build();

      assertEquals(
          Table.builder().name(QualifiedName.of("core", "customers")).build(),
          onlyTable(assemble(rows)));
    }

    @Test
    void itBuildsSchemaWithNoTablesOrEnums() {
      final var rows = PgCatalogRows.builder().schemaNames(List.of("empty")).build();

      assertEquals(
          CatalogSnapshot.builder()
              .productName(PRODUCT_NAME)
              .productVersion(PRODUCT_VERSION)
              .schemas(List.of(Schema.builder().name("empty").build()))
              .build(),
          assemble(rows));
    }

    @Test
    void itBuildsEmptySnapshotWhenNoSchemaWasRead() {
      final var snapshot = assemble(PgCatalogRows.builder().build());

      assertEquals(List.of(), snapshot.schemas());
    }

    @Test
    void itBuildsTableWithoutPrimaryKey() {
      final var table = onlyTable(assemble(customersRows().build()));

      assertTrue(table.primaryKey().isEmpty());
    }

    @Test
    void itMapsPrimaryKeyInKeyOrder() {
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(
                              CUSTOMERS_OID,
                              "customers_pkey",
                              ConstraintType.PRIMARY_KEY,
                              "email",
                              "id")
                          .build()))
              .build();

      assertEquals(
          PrimaryKey.builder().name("customers_pkey").columns(List.of("email", "id")).build(),
          onlyTable(assemble(rows)).primaryKey().orElseThrow());
    }

    @Test
    void itMapsEnumLabelsInRowOrder() {
      final var rows =
          PgCatalogRows.builder()
              .schemaNames(List.of("core"))
              .enums(List.of(new EnumRow("core", "Mood", List.of("sad", "happy", "Two Words"))))
              .build();

      assertEquals(
          List.of(
              EnumType.builder()
                  .name(QualifiedName.of("core", "Mood"))
                  .values(List.of("sad", "happy", "Two Words"))
                  .build()),
          assemble(rows).schemas().getFirst().enumTypes());
    }

    @Test
    void itMapsUserArrayColumnType() {
      final var rows =
          customersRows()
              .tables(List.of(customersTable(1)))
              .columns(
                  List.of(
                      new ColumnRow(
                          CUSTOMERS_OID,
                          "moods",
                          true,
                          new TypeRow("\"Mood\"[]", "core", "_Mood", "core", "Mood"))))
              .build();

      assertEquals(
          DataType.builder()
              .displayText("Mood[]")
              .userType(QualifiedName.of("core", "Mood"))
              .build(),
          onlyTable(assemble(rows)).columns().getFirst().dataType());
    }
  }

  @Nested
  class InvalidRowTests {

    @Test
    void itThrowsExceptionWhenForeignKeyHasNoReferencedTable() {
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(
                              CUSTOMERS_OID, "customers_id_fkey", ConstraintType.FOREIGN_KEY, "id")
                          .referencedColumns(List.of("id"))
                          .build()))
              .build();

      final var exception = assertThrows(IllegalArgumentException.class, () -> assemble(rows));

      assertEquals("Foreign key customers_id_fkey has no referenced table", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenForeignKeyActionIsUnknown() {
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(
                              CUSTOMERS_OID, "customers_id_fkey", ConstraintType.FOREIGN_KEY, "id")
                          .referencedTable(QualifiedName.of("core", "customers"))
                          .referencedColumns(List.of("id"))
                          .onUpdate('z')
                          .build()))
              .build();

      final var exception = assertThrows(IllegalArgumentException.class, () -> assemble(rows));

      assertEquals("Unknown PostgreSQL referential action code 'z'", exception.getMessage());
    }
  }

  @Nested
  class OrphanRowTests {

    @Test
    void itDropsTableWhoseSchemaWasNotRead() {
      final var rows =
          customersRows()
              .tables(List.of(customersTable(1), new TableRow(ORDERS_OID, "gone", "orders", 1)))
              .columns(
                  List.of(
                      new ColumnRow(CUSTOMERS_OID, "id", false, INTEGER),
                      new ColumnRow(ORDERS_OID, "order_id", false, INTEGER)))
              .build();

      final var snapshot = assemble(rows);

      assertEquals(
          List.of(QualifiedName.of("core", "customers")),
          snapshot.schemas().getFirst().tables().stream().map(Table::name).toList());
      assertEquals(
          List.of(
              "Dropped table gone.orders because its schema was not read; the catalog probably"
                  + " changed during the read",
              "Dropped column order_id because its table (OID 200) was not read; the catalog"
                  + " probably changed during the read"),
          warnings());
    }

    @Test
    void itDropsColumnConstraintAndIndexWhoseTableWasNotRead() {
      final var rows =
          customersRows()
              .tables(List.of(customersTable(1)))
              .columns(
                  List.of(
                      new ColumnRow(CUSTOMERS_OID, "id", false, INTEGER),
                      new ColumnRow(UNKNOWN_OID, "ghost", true, TEXT)))
              .constraints(
                  List.of(
                      constraint(UNKNOWN_OID, "ghost_pkey", ConstraintType.PRIMARY_KEY, "ghost")
                          .build()))
              .uniqueIndexes(
                  List.of(new UniqueIndexRow(UNKNOWN_OID, "ghost_idx", List.of("ghost"))))
              .build();

      final var table = onlyTable(assemble(rows));

      assertEquals(List.of("id"), table.columns().stream().map(Column::name).toList());
      assertTrue(table.primaryKey().isEmpty());
      assertEquals(List.of(), table.uniqueKeys());
      assertEquals(
          List.of(
              "Dropped column ghost because its table (OID 999) was not read; the catalog"
                  + " probably changed during the read",
              "Dropped constraint ghost_pkey because its table (OID 999) was not read; the"
                  + " catalog probably changed during the read",
              "Dropped unique index ghost_idx because its table (OID 999) was not read; the"
                  + " catalog probably changed during the read"),
          warnings());
    }

    @Test
    void itDropsConstraintNamingColumnThatWasNotRead() {
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(
                              CUSTOMERS_OID,
                              "customers_pkey",
                              ConstraintType.PRIMARY_KEY,
                              "id",
                              "dropped")
                          .build(),
                      constraint(
                              CUSTOMERS_OID, "customers_email_key", ConstraintType.UNIQUE, "email")
                          .build()))
              .build();

      final var table = onlyTable(assemble(rows));

      assertTrue(table.primaryKey().isEmpty());
      assertEquals(1, table.uniqueKeys().size());
      assertEquals(
          List.of(
              "Dropped key core.customers customers_pkey because it names a column that was not"
                  + " read; the catalog probably changed during the read"),
          warnings());
    }

    @Test
    void itDropsForeignKeyNamingColumnThatWasNotRead() {
      final var rows =
          customersRows()
              .constraints(
                  List.of(
                      constraint(
                              CUSTOMERS_OID,
                              "customers_gone_fkey",
                              ConstraintType.FOREIGN_KEY,
                              "gone")
                          .referencedTable(QualifiedName.of("core", "other"))
                          .referencedColumns(List.of("id"))
                          .build()))
              .build();

      assertEquals(List.of(), onlyTable(assemble(rows)).foreignKeys());
      assertEquals(1, warnings().size());
    }

    @Test
    void itDropsUniqueIndexNamingColumnThatWasNotRead() {
      final var rows =
          customersRows()
              .uniqueIndexes(
                  List.of(
                      new UniqueIndexRow(CUSTOMERS_OID, "customers_gone_idx", List.of("gone")),
                      new UniqueIndexRow(CUSTOMERS_OID, "customers_email_idx", List.of("email"))))
              .build();

      final var table = onlyTable(assemble(rows));

      assertEquals(
          List.of(
              UniqueKey.builder().name("customers_email_idx").columns(List.of("email")).build()),
          table.uniqueKeys());
      assertEquals(
          List.of(
              "Dropped key core.customers customers_gone_idx because it names a column that was"
                  + " not read; the catalog probably changed during the read"),
          warnings());
    }

    @Test
    void itDropsEnumWhoseSchemaWasNotRead() {
      final var rows =
          PgCatalogRows.builder()
              .schemaNames(List.of("core"))
              .enums(
                  List.of(
                      new EnumRow("core", "Mood", List.of("happy")),
                      new EnumRow("gone", "Level", List.of("low"))))
              .build();

      final var enums = assemble(rows).schemas().getFirst().enumTypes();

      assertEquals(
          List.of(QualifiedName.of("core", "Mood")), enums.stream().map(EnumType::name).toList());
      assertEquals(
          List.of(
              "Dropped enum gone.Level because its schema was not read; the catalog probably"
                  + " changed during the read"),
          warnings());
    }

    @Test
    void itSanitisesDroppedNameInWarning() {
      final var rows =
          customersRows()
              .tables(List.of(customersTable(1)))
              .columns(
                  List.of(
                      new ColumnRow(CUSTOMERS_OID, "id", false, INTEGER),
                      new ColumnRow(UNKNOWN_OID, "line\nbreak\r", true, TEXT)))
              .build();

      assemble(rows);

      assertEquals(
          List.of(
              "Dropped column line break because its table (OID 999) was not read; the catalog"
                  + " probably changed during the read"),
          warnings());
    }

    @Test
    void itStillDropsRowWhenWarningsAreDisabled() {
      logCapture.setLoggerLevel(Level.SEVERE);
      final var rows =
          customersRows()
              .tables(List.of(customersTable(1)))
              .columns(
                  List.of(
                      new ColumnRow(CUSTOMERS_OID, "id", false, INTEGER),
                      new ColumnRow(UNKNOWN_OID, "ghost", true, TEXT)))
              .build();

      final var table = onlyTable(assemble(rows));

      assertEquals(List.of("id"), table.columns().stream().map(Column::name).toList());
      assertEquals(List.of(), logCapture.records());
    }
  }

  @Nested
  class ColumnCountTests {

    @Test
    void itDropsTableWhoseColumnRowsAreFewerThanItsColumnCount() {
      final var rows = customersRows().tables(List.of(customersTable(3))).build();

      final var snapshot = assemble(rows);

      assertEquals(List.of(), snapshot.schemas().getFirst().tables());
      assertEquals(
          List.of(
              "Dropped table core.customers because it had 3 columns but 2 column rows were"
                  + " read; the catalog probably changed during the read"),
          warnings());
    }

    @Test
    void itDropsTableWhoseColumnRowsExceedItsColumnCount() {
      final var rows = customersRows().tables(List.of(customersTable(1))).build();

      assertEquals(List.of(), assemble(rows).schemas().getFirst().tables());
      assertEquals(
          List.of(
              "Dropped table core.customers because it had 1 columns but 2 column rows were"
                  + " read; the catalog probably changed during the read"),
          warnings());
    }

    @Test
    void itDropsTableThatHadColumnsWhenNoColumnRowsWereRead() {
      final var rows = customersRows().columns(List.of()).build();

      assertEquals(List.of(), assemble(rows).schemas().getFirst().tables());
      assertEquals(
          List.of(
              "Dropped table core.customers because it had 2 columns but 0 column rows were"
                  + " read; the catalog probably changed during the read"),
          warnings());
    }

    @Test
    void itKeepsGenuineZeroColumnTableWithoutWarning() {
      final var rows =
          customersRows().tables(List.of(customersTable(0))).columns(List.of()).build();

      assertEquals(
          List.of(QualifiedName.of("core", "customers")),
          assemble(rows).schemas().getFirst().tables().stream().map(Table::name).toList());
      assertEquals(List.of(), warnings());
    }

    @Test
    void itLogsOnlyTheTableWhenDroppingItWithItsConstraintsAndIndexes() {
      final var rows =
          customersRows()
              .tables(List.of(customersTable(3)))
              .constraints(
                  List.of(
                      constraint(CUSTOMERS_OID, "customers_pkey", ConstraintType.PRIMARY_KEY, "id")
                          .build(),
                      constraint(CUSTOMERS_OID, "customers_gone_key", ConstraintType.UNIQUE, "gone")
                          .build()))
              .uniqueIndexes(
                  List.of(
                      new UniqueIndexRow(CUSTOMERS_OID, "customers_email_idx", List.of("email"))))
              .build();

      assertEquals(List.of(), assemble(rows).schemas().getFirst().tables());
      assertEquals(
          List.of(
              "Dropped table core.customers because it had 3 columns but 2 column rows were"
                  + " read; the catalog probably changed during the read"),
          warnings());
    }

    @Test
    void itKeepsOtherTablesWhenOneIsDropped() {
      final var rows =
          customersRows()
              .tables(List.of(customersTable(2), new TableRow(ORDERS_OID, "core", "orders", 1)))
              .build();

      assertEquals(
          List.of(QualifiedName.of("core", "customers")),
          assemble(rows).schemas().getFirst().tables().stream().map(Table::name).toList());
      assertEquals(1, warnings().size());
    }
  }

  @Nested
  class AbsentSchemaTests {

    @Test
    void itLogsWhetherEachAbsentRequestedSchemaIsMissingOrLacksUsage() {
      final var rows =
          customersRows()
              .requestedSchemas(List.of("nope", "core", "secret"))
              .schemasWithoutUsage(List.of("secret"))
              .build();

      final var snapshot = assemble(rows);

      assertEquals(List.of("core"), snapshot.schemas().stream().map(Schema::name).toList());
      assertEquals(
          List.of(
              "Requested schema nope is not in the snapshot because it does not exist",
              "Requested schema secret is not in the snapshot because the role has no USAGE"
                  + " privilege on it"),
          warnings());
    }

    @Test
    void itLogsNothingWhenEveryRequestedSchemaIsPresent() {
      final var rows = customersRows().requestedSchemas(List.of("core")).build();

      assemble(rows);

      assertEquals(List.of(), warnings());
    }

    @Test
    void itLogsNothingWhenNoSchemasWereRequested() {
      final var rows = customersRows().schemasWithoutUsage(List.of("secret")).build();

      assemble(rows);

      assertEquals(List.of(), warnings());
    }

    @Test
    void itSanitisesAbsentSchemaNameInWarning() {
      final var rows = customersRows().requestedSchemas(List.of("line\nbreak\r")).build();

      assemble(rows);

      assertEquals(
          List.of("Requested schema line break is not in the snapshot because it does not exist"),
          warnings());
    }
  }

  @Nested
  class DuplicateColumnKeyTests {

    @Test
    void itSkipsUniqueIndexListingColumnTwice() {
      final var rows =
          customersRows()
              .uniqueIndexes(
                  List.of(
                      new UniqueIndexRow(
                          CUSTOMERS_OID, "customers_email_email_idx", List.of("email", "email")),
                      new UniqueIndexRow(CUSTOMERS_OID, "customers_id_idx", List.of("id"))))
              .build();

      final var table = onlyTable(assemble(rows));

      assertEquals(
          List.of(UniqueKey.builder().name("customers_id_idx").columns(List.of("id")).build()),
          table.uniqueKeys());
      assertEquals(
          List.of(
              "Skipped key core.customers customers_email_email_idx because it lists a column"
                  + " more than once"),
          warnings());
    }
  }
}
