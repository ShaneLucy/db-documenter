package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.QueryRunnerFactory;
import db.documenter.internal.queries.api.QueryRunner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchemaBuilderTest {

  @Mock private ConnectionManager connectionManager;
  @Mock private QueryRunnerFactory queryRunnerFactory;
  @Mock private EnumBuilder enumBuilder;
  @Mock private TableBuilder tableBuilder;
  @Mock private Connection connection;
  @Mock private QueryRunner queryRunner;

  private SchemaBuilder schemaBuilder;

  @BeforeEach
  void setUp() {
    reset(
        connectionManager, queryRunnerFactory, enumBuilder, tableBuilder, connection, queryRunner);
    schemaBuilder =
        new SchemaBuilder(connectionManager, queryRunnerFactory, enumBuilder, tableBuilder);
  }

  @Nested
  class BuildSchemasTests {

    @Test
    void returnsEmptyListWhenNoSchemasProvided() throws SQLException {
      final List<Schema> result = schemaBuilder.buildSchemas(List.of());

      assertTrue(result.isEmpty());
    }

    @Test
    void buildsSchemaWithTablesAndEnums() throws SQLException {
      final DbEnum dbEnum =
          DbEnum.builder()
              .enumName("status")
              .columnName("status")
              .enumValues(List.of("active", "inactive"))
              .build();

      final Table table =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();

      when(connectionManager.getConnection()).thenReturn(connection);
      when(queryRunnerFactory.createQueryRunner(connection)).thenReturn(queryRunner);
      when(enumBuilder.buildEnums(queryRunner, "test_schema")).thenReturn(List.of(dbEnum));
      when(tableBuilder.buildTables(queryRunner, "test_schema", List.of(dbEnum)))
          .thenReturn(List.of(table));

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("test_schema"));

      assertEquals(1, result.size());
      assertEquals("test_schema", result.get(0).name());
      assertEquals(List.of(dbEnum), result.get(0).dbEnums());
      assertEquals(List.of(table), result.get(0).tables());

      verify(connection).close();
    }

    @Test
    void buildsMultipleSchemas() throws SQLException {
      final DbEnum dbEnum1 = DbEnum.builder().enumName("status").columnName("status").build();
      final DbEnum dbEnum2 = DbEnum.builder().enumName("role").columnName("role").build();

      final Table table1 =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();
      final Table table2 =
          Table.builder().name("orders").columns(List.of()).foreignKeys(List.of()).build();

      when(connectionManager.getConnection()).thenReturn(connection);
      when(queryRunnerFactory.createQueryRunner(connection)).thenReturn(queryRunner);

      when(enumBuilder.buildEnums(queryRunner, "schema1")).thenReturn(List.of(dbEnum1));
      when(tableBuilder.buildTables(queryRunner, "schema1", List.of(dbEnum1)))
          .thenReturn(List.of(table1));

      when(enumBuilder.buildEnums(queryRunner, "schema2")).thenReturn(List.of(dbEnum2));
      when(tableBuilder.buildTables(queryRunner, "schema2", List.of(dbEnum2)))
          .thenReturn(List.of(table2));

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("schema1", "schema2"));

      assertEquals(2, result.size());
      assertEquals("schema1", result.get(0).name());
      assertEquals(List.of(dbEnum1), result.get(0).dbEnums());
      assertEquals(List.of(table1), result.get(0).tables());
      assertEquals("schema2", result.get(1).name());
      assertEquals(List.of(dbEnum2), result.get(1).dbEnums());
      assertEquals(List.of(table2), result.get(1).tables());
    }

    @Test
    void buildsSchemaWithNoEnums() throws SQLException {
      final Table table =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();

      when(connectionManager.getConnection()).thenReturn(connection);
      when(queryRunnerFactory.createQueryRunner(connection)).thenReturn(queryRunner);
      when(enumBuilder.buildEnums(queryRunner, "test_schema")).thenReturn(List.of());
      when(tableBuilder.buildTables(queryRunner, "test_schema", List.of()))
          .thenReturn(List.of(table));

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("test_schema"));

      assertEquals(1, result.size());
      assertTrue(result.get(0).dbEnums().isEmpty());
      assertEquals(List.of(table), result.get(0).tables());
    }

    @Test
    void buildsSchemaWithNoTables() throws SQLException {
      final DbEnum dbEnum = DbEnum.builder().enumName("status").columnName("status").build();

      when(connectionManager.getConnection()).thenReturn(connection);
      when(queryRunnerFactory.createQueryRunner(connection)).thenReturn(queryRunner);
      when(enumBuilder.buildEnums(queryRunner, "test_schema")).thenReturn(List.of(dbEnum));
      when(tableBuilder.buildTables(queryRunner, "test_schema", List.of(dbEnum)))
          .thenReturn(List.of());

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("test_schema"));

      assertEquals(1, result.size());
      assertEquals(List.of(dbEnum), result.get(0).dbEnums());
      assertTrue(result.get(0).tables().isEmpty());
    }

    @Test
    void closesConnectionEvenWhenExceptionOccurs() throws SQLException {
      when(connectionManager.getConnection()).thenReturn(connection);
      when(queryRunnerFactory.createQueryRunner(connection)).thenReturn(queryRunner);
      when(enumBuilder.buildEnums(queryRunner, "test_schema"))
          .thenThrow(new SQLException("Database error"));

      assertThrows(SQLException.class, () -> schemaBuilder.buildSchemas(List.of("test_schema")));

      verify(connection).close();
    }

    @Test
    void propagatesSQLExceptionFromEnumBuilder() throws SQLException {
      when(connectionManager.getConnection()).thenReturn(connection);
      when(queryRunnerFactory.createQueryRunner(connection)).thenReturn(queryRunner);
      when(enumBuilder.buildEnums(queryRunner, "test_schema"))
          .thenThrow(new SQLException("Failed to build enums"));

      final SQLException exception =
          assertThrows(
              SQLException.class, () -> schemaBuilder.buildSchemas(List.of("test_schema")));

      assertEquals("Failed to build enums", exception.getMessage());
    }

    @Test
    void propagatesSQLExceptionFromTableBuilder() throws SQLException {
      when(connectionManager.getConnection()).thenReturn(connection);
      when(queryRunnerFactory.createQueryRunner(connection)).thenReturn(queryRunner);
      when(enumBuilder.buildEnums(queryRunner, "test_schema")).thenReturn(List.of());
      when(tableBuilder.buildTables(queryRunner, "test_schema", List.of()))
          .thenThrow(new SQLException("Failed to build tables"));

      final SQLException exception =
          assertThrows(
              SQLException.class, () -> schemaBuilder.buildSchemas(List.of("test_schema")));

      assertEquals("Failed to build tables", exception.getMessage());
    }
  }
}
