package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import db.documenter.DatabaseType;
import db.documenter.DbDocumenterConfig;
import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
class SchemaBuilderTest {

  @Mock private ConnectionManager connectionManager;
  @Mock private EnumBuilder enumBuilder;
  @Mock private CompositeTypeBuilder compositeTypeBuilder;
  @Mock private TableBuilder tableBuilder;
  @Mock private ViewBuilder viewBuilder;
  @Mock private Connection connection;
  @Mock private PreparedStatement preparedStatement;
  @Mock private ResultSet resultSet;

  private SchemaBuilder schemaBuilder;

  private static final DbDocumenterConfig CONFIG =
      DbDocumenterConfig.builder()
          .schemas(List.of("test_schema"))
          .databaseHost("localhost")
          .databasePort(5432)
          .databaseName("testdb")
          .useSsl(false)
          .username("user")
          .password("pass")
          .databaseType(DatabaseType.POSTGRESQL)
          .build();

  @BeforeEach
  void setUp() throws SQLException {
    reset(
        connectionManager,
        enumBuilder,
        compositeTypeBuilder,
        tableBuilder,
        viewBuilder,
        connection,
        preparedStatement,
        resultSet);
    schemaBuilder =
        new PostgresqlSchemaBuilder(
            CONFIG,
            connectionManager,
            enumBuilder,
            compositeTypeBuilder,
            tableBuilder,
            viewBuilder);
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
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of("active", "inactive"))
              .build();

      final Table table =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();

      final Map<ColumnKey, UdtReference> columnUdtMappings = Map.of();

      // The PostgresqlQueryRunner calls connection.prepareStatement for getColumnUdtMappings.
      // All other query calls are handled by the mocked builders.
      when(connectionManager.getConnection()).thenReturn(connection);
      when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
      when(preparedStatement.executeQuery()).thenReturn(resultSet);
      when(resultSet.next()).thenReturn(false);

      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of(dbEnum));
      when(compositeTypeBuilder.buildCompositeTypes(
              any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of());
      when(tableBuilder.buildTables(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of(table));
      when(viewBuilder.buildViews(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of());
      when(viewBuilder.buildMaterializedViews(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of());

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("test_schema"));

      assertEquals(1, result.size());
      assertEquals("test_schema", result.getFirst().name());
      assertEquals(List.of(dbEnum), result.getFirst().dbEnums());
      assertEquals(List.of(table), result.getFirst().tables());
      assertTrue(result.getFirst().views().isEmpty());
      assertTrue(result.getFirst().materializedViews().isEmpty());

      verify(connection).close();
    }

    @Test
    void buildsMultipleSchemas() throws SQLException {
      final DbEnum dbEnum1 =
          DbEnum.builder().schemaName("schema1").enumName("status").enumValues(List.of()).build();
      final DbEnum dbEnum2 =
          DbEnum.builder().schemaName("schema2").enumName("role").enumValues(List.of()).build();

      final Table table1 =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();
      final Table table2 =
          Table.builder().name("orders").columns(List.of()).foreignKeys(List.of()).build();

      when(connectionManager.getConnection()).thenReturn(connection);
      when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
      when(preparedStatement.executeQuery()).thenReturn(resultSet);
      when(resultSet.next()).thenReturn(false);

      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("schema1")))
          .thenReturn(List.of(dbEnum1));
      when(compositeTypeBuilder.buildCompositeTypes(
              any(PostgresqlQueryRunner.class), eq("schema1")))
          .thenReturn(List.of());
      when(tableBuilder.buildTables(any(PostgresqlQueryRunner.class), eq("schema1"), any(), any()))
          .thenReturn(List.of(table1));
      when(viewBuilder.buildViews(any(PostgresqlQueryRunner.class), eq("schema1"), any(), any()))
          .thenReturn(List.of());
      when(viewBuilder.buildMaterializedViews(
              any(PostgresqlQueryRunner.class), eq("schema1"), any(), any()))
          .thenReturn(List.of());

      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("schema2")))
          .thenReturn(List.of(dbEnum2));
      when(compositeTypeBuilder.buildCompositeTypes(
              any(PostgresqlQueryRunner.class), eq("schema2")))
          .thenReturn(List.of());
      when(tableBuilder.buildTables(any(PostgresqlQueryRunner.class), eq("schema2"), any(), any()))
          .thenReturn(List.of(table2));
      when(viewBuilder.buildViews(any(PostgresqlQueryRunner.class), eq("schema2"), any(), any()))
          .thenReturn(List.of());
      when(viewBuilder.buildMaterializedViews(
              any(PostgresqlQueryRunner.class), eq("schema2"), any(), any()))
          .thenReturn(List.of());

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("schema1", "schema2"));

      assertEquals(2, result.size());
      assertEquals("schema1", result.getFirst().name());
      assertEquals(List.of(dbEnum1), result.getFirst().dbEnums());
      assertEquals(List.of(table1), result.getFirst().tables());
      assertEquals("schema2", result.get(1).name());
      assertEquals(List.of(dbEnum2), result.get(1).dbEnums());
      assertEquals(List.of(table2), result.get(1).tables());
    }

    @Test
    void buildsSchemaWithNoEnums() throws SQLException {
      final Table table =
          Table.builder().name("users").columns(List.of()).foreignKeys(List.of()).build();

      when(connectionManager.getConnection()).thenReturn(connection);
      when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
      when(preparedStatement.executeQuery()).thenReturn(resultSet);
      when(resultSet.next()).thenReturn(false);

      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of());
      when(compositeTypeBuilder.buildCompositeTypes(
              any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of());
      when(tableBuilder.buildTables(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of(table));
      when(viewBuilder.buildViews(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of());
      when(viewBuilder.buildMaterializedViews(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of());

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("test_schema"));

      assertEquals(1, result.size());
      assertTrue(result.getFirst().dbEnums().isEmpty());
      assertEquals(List.of(table), result.getFirst().tables());
    }

    @Test
    void buildsSchemaWithNoTables() throws SQLException {
      final DbEnum dbEnum =
          DbEnum.builder()
              .schemaName("test_schema")
              .enumName("status")
              .enumValues(List.of())
              .build();

      when(connectionManager.getConnection()).thenReturn(connection);
      when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
      when(preparedStatement.executeQuery()).thenReturn(resultSet);
      when(resultSet.next()).thenReturn(false);

      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of(dbEnum));
      when(compositeTypeBuilder.buildCompositeTypes(
              any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of());
      when(tableBuilder.buildTables(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of());
      when(viewBuilder.buildViews(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of());
      when(viewBuilder.buildMaterializedViews(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenReturn(List.of());

      final List<Schema> result = schemaBuilder.buildSchemas(List.of("test_schema"));

      assertEquals(1, result.size());
      assertEquals(List.of(dbEnum), result.getFirst().dbEnums());
      assertTrue(result.getFirst().tables().isEmpty());
    }

    @Test
    void closesConnectionEvenWhenExceptionOccurs() throws SQLException {
      when(connectionManager.getConnection()).thenReturn(connection);
      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenThrow(new SQLException("Database error"));

      assertThrows(SQLException.class, () -> schemaBuilder.buildSchemas(List.of("test_schema")));

      verify(connection).close();
    }

    @Test
    void propagatesSQLExceptionFromEnumBuilder() throws SQLException {
      when(connectionManager.getConnection()).thenReturn(connection);
      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenThrow(new SQLException("Failed to build enums"));

      final SQLException exception =
          assertThrows(
              SQLException.class, () -> schemaBuilder.buildSchemas(List.of("test_schema")));

      assertEquals("Failed to build enums", exception.getMessage());
    }

    @Test
    void propagatesSQLExceptionFromTableBuilder() throws SQLException {
      when(connectionManager.getConnection()).thenReturn(connection);
      when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
      when(preparedStatement.executeQuery()).thenReturn(resultSet);
      when(resultSet.next()).thenReturn(false);

      when(enumBuilder.buildEnums(any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of());
      when(compositeTypeBuilder.buildCompositeTypes(
              any(PostgresqlQueryRunner.class), eq("test_schema")))
          .thenReturn(List.of());
      when(tableBuilder.buildTables(
              any(PostgresqlQueryRunner.class), eq("test_schema"), any(), any()))
          .thenThrow(new SQLException("Failed to build tables"));

      final SQLException exception =
          assertThrows(
              SQLException.class, () -> schemaBuilder.buildSchemas(List.of("test_schema")));

      assertEquals("Failed to build tables", exception.getMessage());
    }
  }
}
