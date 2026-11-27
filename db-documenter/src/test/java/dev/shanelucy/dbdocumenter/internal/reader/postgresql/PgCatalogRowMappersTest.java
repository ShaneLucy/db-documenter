package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ColumnRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.ConstraintRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.EnumRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TableRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TypeRow;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.UniqueIndexRow;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Unit tests for {@link PgCatalogRowMappers} against a mocked {@link ResultSet}.
 *
 * <p>Covers the defensive branches a real server never reaches: a NULL or non-{@code String[]}
 * array, a NULL or empty {@code "char"} code, and a referenced schema without a table. The SQL that
 * feeds the mappers is proven by {@code PgCatalogReaderIT}.
 */
class PgCatalogRowMappersTest {

  private ResultSet resultSet;

  @BeforeEach
  void setUp() {
    resultSet = mock(ResultSet.class);
  }

  private static Array driverArray(final Object values) throws SQLException {
    final var array = mock(Array.class);
    when(array.getArray()).thenReturn(values);
    return array;
  }

  /** Stubs a constraint row with key column {@code id}; returns the {@code column_names} array. */
  private Array givenConstraint(final String type) throws SQLException {
    when(resultSet.getLong("table_oid")).thenReturn(42L);
    when(resultSet.getString("constraint_name")).thenReturn("customers_key");
    when(resultSet.getString("constraint_type")).thenReturn(type);
    final var keyColumns = driverArray(new String[] {"id"});
    when(resultSet.getArray("column_names")).thenReturn(keyColumns);
    return keyColumns;
  }

  @Nested
  class MapTableTests {

    @Test
    void itMapsTableRowWithColumnCount() throws SQLException {
      when(resultSet.getLong("table_oid")).thenReturn(42L);
      when(resultSet.getString("schema_name")).thenReturn("core");
      when(resultSet.getString("table_name")).thenReturn("customers");
      when(resultSet.getInt("column_count")).thenReturn(3);

      assertEquals(
          new TableRow(42L, "core", "customers", 3), PgCatalogRowMappers.mapTable(resultSet));
    }
  }

  @Nested
  class MapColumnTests {

    @Test
    void itMapsPgCatalogColumnWithoutElementType() throws SQLException {
      when(resultSet.getLong("table_oid")).thenReturn(42L);
      when(resultSet.getString("column_name")).thenReturn("id");
      when(resultSet.getBoolean("is_nullable")).thenReturn(false);
      when(resultSet.getString("formatted_type")).thenReturn("integer");
      when(resultSet.getString("type_schema")).thenReturn("pg_catalog");
      when(resultSet.getString("type_name")).thenReturn("int4");

      assertEquals(
          new ColumnRow(42L, "id", false, new TypeRow("integer", "pg_catalog", "int4", null, null)),
          PgCatalogRowMappers.mapColumn(resultSet));
    }

    @Test
    void itMapsUserArrayColumnWithElementType() throws SQLException {
      when(resultSet.getLong("table_oid")).thenReturn(42L);
      when(resultSet.getString("column_name")).thenReturn("moods");
      when(resultSet.getBoolean("is_nullable")).thenReturn(true);
      when(resultSet.getString("formatted_type")).thenReturn("\"Mood\"[]");
      when(resultSet.getString("type_schema")).thenReturn("core");
      when(resultSet.getString("type_name")).thenReturn("_Mood");
      when(resultSet.getString("element_schema")).thenReturn("core");
      when(resultSet.getString("element_name")).thenReturn("Mood");

      assertEquals(
          new ColumnRow(
              42L, "moods", true, new TypeRow("\"Mood\"[]", "core", "_Mood", "core", "Mood")),
          PgCatalogRowMappers.mapColumn(resultSet));
    }
  }

  @Nested
  class MapConstraintTests {

    @ParameterizedTest
    @CsvSource({"p,PRIMARY_KEY", "u,UNIQUE", "f,FOREIGN_KEY"})
    void itParsesConstraintType(final String code, final ConstraintType expected)
        throws SQLException {
      givenConstraint(code);

      assertEquals(expected, PgCatalogRowMappers.mapConstraint(resultSet).type());
    }

    @Test
    void itMapsForeignKeyRow() throws SQLException {
      givenConstraint("f");
      when(resultSet.getString("referenced_schema")).thenReturn("external_ref");
      when(resultSet.getString("referenced_table")).thenReturn("accounts");
      final var referenced = driverArray(new String[] {"account_id"});
      when(resultSet.getArray("referenced_column_names")).thenReturn(referenced);
      when(resultSet.getString("on_delete")).thenReturn("c");
      when(resultSet.getString("on_update")).thenReturn("n");

      assertEquals(
          ConstraintRow.builder()
              .tableOid(42L)
              .name("customers_key")
              .type(ConstraintType.FOREIGN_KEY)
              .columns(List.of("id"))
              .referencedTable(QualifiedName.of("external_ref", "accounts"))
              .referencedColumns(List.of("account_id"))
              .onDelete('c')
              .onUpdate('n')
              .build(),
          PgCatalogRowMappers.mapConstraint(resultSet));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void itThrowsExceptionWhenConstraintTypeIsMissing(final String type) throws SQLException {
      givenConstraint(type);

      final var exception =
          assertThrows(
              IllegalArgumentException.class, () -> PgCatalogRowMappers.mapConstraint(resultSet));

      assertEquals("Unknown PostgreSQL constraint type ' '", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenConstraintTypeIsUnknown() throws SQLException {
      givenConstraint("c");

      final var exception =
          assertThrows(
              IllegalArgumentException.class, () -> PgCatalogRowMappers.mapConstraint(resultSet));

      assertEquals("Unknown PostgreSQL constraint type 'c'", exception.getMessage());
    }

    @Test
    void itReadsNullAndEmptyActionCodesAsSpace() throws SQLException {
      givenConstraint("p");
      when(resultSet.getString("on_delete")).thenReturn(null);
      when(resultSet.getString("on_update")).thenReturn("");

      final var row = PgCatalogRowMappers.mapConstraint(resultSet);

      assertEquals(' ', row.onDelete());
      assertEquals(' ', row.onUpdate());
    }

    @Test
    void itReadsOnlyFirstCharacterOfCode() throws SQLException {
      givenConstraint("pk");
      when(resultSet.getString("on_delete")).thenReturn("cascade");

      final var row = PgCatalogRowMappers.mapConstraint(resultSet);

      assertEquals(ConstraintType.PRIMARY_KEY, row.type());
      assertEquals('c', row.onDelete());
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = "NULL",
        value = {"external_ref,NULL", "NULL,accounts", "NULL,NULL"})
    void itHasNoReferencedTableUnlessSchemaAndTableAreBothPresent(
        final String schema, final String table) throws SQLException {
      givenConstraint("f");
      when(resultSet.getString("referenced_schema")).thenReturn(schema);
      when(resultSet.getString("referenced_table")).thenReturn(table);

      assertNull(PgCatalogRowMappers.mapConstraint(resultSet).referencedTable());
    }
  }

  @Nested
  class TextArrayTests {

    @Test
    void itReadsNullArrayAsEmptyList() throws SQLException {
      givenConstraint("p");
      when(resultSet.getArray("referenced_column_names")).thenReturn(null);

      assertEquals(List.of(), PgCatalogRowMappers.mapConstraint(resultSet).referencedColumns());
    }

    @Test
    void itFreesEachDriverArrayAfterReadingIt() throws SQLException {
      final var keyColumns = givenConstraint("p");
      final var referenced = driverArray(new String[0]);
      when(resultSet.getArray("referenced_column_names")).thenReturn(referenced);

      PgCatalogRowMappers.mapConstraint(resultSet);

      verify(keyColumns).free();
      verify(referenced).free();
    }

    @Test
    void itThrowsExceptionAndFreesArrayWhenArrayIsNotText() throws SQLException {
      givenConstraint("p");
      final var integers = driverArray(new Integer[] {1});
      when(resultSet.getArray("column_names")).thenReturn(integers);

      final var exception =
          assertThrows(SQLException.class, () -> PgCatalogRowMappers.mapConstraint(resultSet));

      assertEquals("Expected a text[] value in column column_names", exception.getMessage());
      verify(integers).free();
    }

    @Test
    void itThrowsExceptionWhenArrayHoldsNull() throws SQLException {
      when(resultSet.getString("schema_name")).thenReturn("core");
      when(resultSet.getString("enum_name")).thenReturn("Mood");
      final var labels = driverArray(new String[] {"happy", null});
      when(resultSet.getArray("labels")).thenReturn(labels);

      final var exception =
          assertThrows(
              IllegalArgumentException.class, () -> PgCatalogRowMappers.mapEnum(resultSet));

      assertEquals("labels must not contain null elements", exception.getMessage());
      verify(labels).free();
    }
  }

  @Nested
  class MapUniqueIndexAndEnumTests {

    @Test
    void itMapsUniqueIndexRow() throws SQLException {
      when(resultSet.getLong("table_oid")).thenReturn(42L);
      when(resultSet.getString("index_name")).thenReturn("customers_email_idx");
      final var columns = driverArray(new String[] {"email", "id"});
      when(resultSet.getArray("column_names")).thenReturn(columns);

      assertEquals(
          new UniqueIndexRow(42L, "customers_email_idx", List.of("email", "id")),
          PgCatalogRowMappers.mapUniqueIndex(resultSet));
    }

    @Test
    void itMapsEnumRowWithLabelsInOrder() throws SQLException {
      when(resultSet.getString("schema_name")).thenReturn("core");
      when(resultSet.getString("enum_name")).thenReturn("Mood");
      final var labels = driverArray(new String[] {"sad", "happy"});
      when(resultSet.getArray("labels")).thenReturn(labels);

      assertEquals(
          new EnumRow("core", "Mood", List.of("sad", "happy")),
          PgCatalogRowMappers.mapEnum(resultSet));
    }
  }
}
