package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.Schema;
import dev.shanelucy.dbdocumenter.catalog.Table;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.reader.ReadRequest;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JDBC-level unit tests for {@link PgCatalogReader} against mocked statements and result sets: the
 * statement sequence, parameter binding, resource release and the split of the schema rows by
 * {@code has_usage}. Row mapping is covered by {@link PgCatalogRowMappersTest} and the SQL by
 * {@code PgCatalogReaderIT}.
 */
class PgCatalogReaderTest {

  private static final long TABLE_OID = 42L;
  private static final ReadRequest REQUEST = new ReadRequest(List.of("core"));

  private Connection connection;
  private PreparedStatement statement;
  private Array schemaArray;
  private ResultSet schemas;
  private ResultSet tables;
  private ResultSet columns;
  private ResultSet empty;
  private PgCatalogReader reader;
  private LogCapture assemblerLog;
  private LogCapture readerLog;

  @BeforeEach
  void setUp() throws SQLException {
    assemblerLog = LogCapture.attach(PgCatalogAssembler.class.getName());
    readerLog = LogCapture.attach(PgCatalogReader.class.getName());
    connection = mock(Connection.class);
    final var metaData = mock(DatabaseMetaData.class);
    statement = mock(PreparedStatement.class);
    schemaArray = mock(Array.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
    when(metaData.getDatabaseProductVersion()).thenReturn("18.7");
    when(connection.createArrayOf(eq("text"), any(Object[].class))).thenReturn(schemaArray);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    schemas = resultSet(1);
    when(schemas.getString("schema_name")).thenReturn("core");
    when(schemas.getBoolean("has_usage")).thenReturn(true);
    tables = resultSet(1);
    when(tables.getLong("table_oid")).thenReturn(TABLE_OID);
    when(tables.getString("schema_name")).thenReturn("core");
    when(tables.getString("table_name")).thenReturn("customers");
    when(tables.getInt("column_count")).thenReturn(1);
    columns = resultSet(1);
    when(columns.getLong("table_oid")).thenReturn(TABLE_OID);
    when(columns.getString("column_name")).thenReturn("id");
    when(columns.getString("formatted_type")).thenReturn("integer");
    when(columns.getString("type_schema")).thenReturn("pg_catalog");
    when(columns.getString("type_name")).thenReturn("int4");
    empty = resultSet(0);
    givenResultSets(schemas, tables, columns, empty, empty, empty);

    reader = new PgCatalogReader();
  }

  @AfterEach
  void tearDown() {
    assemblerLog.detach();
    readerLog.detach();
  }

  /** A result set that yields {@code rowCount} rows, each answering the same stubbed values. */
  private static ResultSet resultSet(final int rowCount) throws SQLException {
    final var resultSet = mock(ResultSet.class);
    if (rowCount == 0) {
      when(resultSet.next()).thenReturn(false);
    } else {
      final var more = new Boolean[rowCount];
      Arrays.fill(more, Boolean.TRUE);
      more[rowCount - 1] = Boolean.FALSE;
      when(resultSet.next()).thenReturn(true, more);
    }
    return resultSet;
  }

  private void givenResultSets(final ResultSet first, final ResultSet... rest) throws SQLException {
    when(statement.executeQuery()).thenReturn(first, rest);
  }

  @Nested
  class ReadTests {

    @Test
    void itReadsSnapshotFromSixStatementsBindingSchemaArrayToEach() throws SQLException {
      final var snapshot = reader.read(connection, REQUEST);

      assertEquals("PostgreSQL", snapshot.productName());
      assertEquals("18.7", snapshot.productVersion());
      assertEquals(
          List.of(QualifiedName.of("core", "customers")),
          snapshot.schemas().getFirst().tables().stream().map(Table::name).toList());
      verify(connection, times(6)).prepareStatement(anyString());
      verify(statement, times(6)).setArray(1, schemaArray);
      verify(statement, times(6)).executeQuery();
      verify(statement, times(6)).close();
      verify(schemaArray).free();
      assertEquals(List.of(), assemblerLog.warnings());
    }

    @Test
    void itPassesRequestedSchemasAsTextArray() throws SQLException {
      reader.read(connection, new ReadRequest(List.of("core", "Odd Names")));

      verify(connection).createArrayOf("text", new Object[] {"core", "Odd Names"});
    }

    @Test
    void itDoesNotCloseOrReconfigureConnection() throws SQLException {
      reader.read(connection, REQUEST);

      verify(connection, never()).close();
      verify(connection, never()).setAutoCommit(anyBoolean());
      verify(connection, never()).setTransactionIsolation(anyInt());
      verify(connection, never()).setSchema(anyString());
      verify(connection, never()).commit();
      verify(connection, never()).rollback();
    }

    @Test
    void itLogsRowCountsWhenFineLoggingIsEnabled() throws SQLException {
      readerLog.setLoggerLevel(Level.FINE);

      reader.read(connection, REQUEST);

      final var fine =
          readerLog.records().stream()
              .filter(logRecord -> logRecord.getLevel() == Level.FINE)
              .toList();
      assertEquals(1, fine.size());
      assertEquals(List.of(1, 1, 1, 0, 0, 0), Arrays.asList(fine.getFirst().getParameters()));
    }

    @Test
    void itLogsNoRowCountsByDefault() throws SQLException {
      readerLog.setLoggerLevel(Level.INFO);

      reader.read(connection, REQUEST);

      assertEquals(List.of(), readerLog.records());
    }

    @Test
    void itFreesSchemaArrayWhenQueryFails() throws SQLException {
      final var failure = new SQLException("permission denied");
      when(statement.executeQuery()).thenReturn(schemas).thenThrow(failure);

      final var exception =
          assertThrows(SQLException.class, () -> reader.read(connection, REQUEST));

      assertSame(failure, exception);
      verify(schemaArray).free();
      verify(statement, times(2)).close();
    }

    @Test
    void itFreesSchemaArrayWhenRowIsInvalid() throws SQLException {
      when(tables.getInt("column_count")).thenReturn(-1);

      final var exception =
          assertThrows(ValidationException.class, () -> reader.read(connection, REQUEST));

      assertEquals("columnCount must not be negative, but is -1", exception.getMessage());
      verify(schemaArray).free();
    }

    @Test
    void itThrowsExceptionWhenConnectionIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> reader.read(null, REQUEST));

      assertEquals("connection must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenRequestIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> reader.read(connection, null));

      assertEquals("request must not be null", exception.getMessage());
    }
  }

  @Nested
  class SchemaUsageTests {

    @Test
    void itOmitsSchemaWithoutUsageAndLogsWhy() throws SQLException {
      final var schemaRows = resultSet(2);
      when(schemaRows.getString("schema_name")).thenReturn("core", "secret");
      when(schemaRows.getBoolean("has_usage")).thenReturn(true, false);
      givenResultSets(schemaRows, tables, columns, empty, empty, empty);

      final var snapshot = reader.read(connection, new ReadRequest(List.of("core", "secret")));

      assertEquals(List.of("core"), snapshot.schemas().stream().map(Schema::name).toList());
      assertEquals(
          List.of(
              "Requested schema secret is not in the snapshot because the role has no USAGE"
                  + " privilege on it"),
          assemblerLog.warnings());
    }

    @Test
    void itLogsRequestedSchemaThatTheSchemasQueryDidNotReturn() throws SQLException {
      final var snapshot = reader.read(connection, new ReadRequest(List.of("core", "nope")));

      assertEquals(List.of("core"), snapshot.schemas().stream().map(Schema::name).toList());
      assertEquals(
          List.of("Requested schema nope is not in the snapshot because it does not exist"),
          assemblerLog.warnings());
    }
  }
}
