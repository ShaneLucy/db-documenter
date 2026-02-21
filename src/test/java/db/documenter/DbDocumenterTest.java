package db.documenter;

import static db.documenter.testutils.PumlComparison.comparePumlLineByLine;

import db.documenter.internal.test.helpers.PostgresTestEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.*;

class DbDocumenterTest {

  private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
      new PostgresTestEnvironment();
  private DbDocumenter dbDocumenter;
  private static Connection connection;

  @Nested
  class PostgresqlSchemaToPumlTests {

    @Nested
    class GeneratePumlSingleSchemaTests {

      @BeforeAll
      static void containerSetUp() throws SQLException, IOException {
        POSTGRES_TEST_ENVIRONMENT.startContainer();
        connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
        POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(
            connection, "/postgresql/single-schema/postgresql-specific-single-schema.sql");
      }

      @AfterAll
      static void containerClearDown() throws SQLException {
        connection.close();
        POSTGRES_TEST_ENVIRONMENT.stop();
      }

      @BeforeEach
      void setUp() {
        final var container = POSTGRES_TEST_ENVIRONMENT.getContainer();
        final var dbDocumenterConfig =
            DbDocumenterConfig.builder()
                .databaseName(container.getDatabaseName())
                .username(container.getUsername())
                .schemas(List.of("public"))
                .password(container.getPassword())
                .databasePort(container.getFirstMappedPort())
                .databaseHost(container.getHost())
                .useSsl(false)
                .build();

        dbDocumenter = new DbDocumenter(dbDocumenterConfig);
      }

      @Test
      void itWorksCorrectlyForASingleSchema() throws IOException, SQLException {
        final var expected =
            Files.readString(
                Path.of(
                    "src/test/resources/postgresql/single-schema/postgresql-specific-single-schema-expected-output.puml"));
        final var expectedLines = expected.lines().toList();

        final var result = dbDocumenter.generatePuml();
        comparePumlLineByLine(result, expectedLines);
      }
    }

    @Nested
    class GeneratePumlMultipleSchemaSchemaSqlAgnosticTests {

      @BeforeAll
      static void containerSetUp() throws SQLException, IOException {
        POSTGRES_TEST_ENVIRONMENT.startContainer();
        connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
        POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(
            connection, "/sql-agnostic-multiple-schema.sql");
      }

      @AfterAll
      static void containerClearDown() throws SQLException {
        connection.close();
        POSTGRES_TEST_ENVIRONMENT.stop();
      }

      @BeforeEach
      void setUp() {
        final var container = POSTGRES_TEST_ENVIRONMENT.getContainer();
        final var dbDocumenterConfig =
            DbDocumenterConfig.builder()
                .databaseName(container.getDatabaseName())
                .username(container.getUsername())
                .schemas(List.of("ecommerce", "inventory", "analytics"))
                .password(container.getPassword())
                .databasePort(container.getFirstMappedPort())
                .databaseHost(container.getHost())
                .useSsl(false)
                .build();

        dbDocumenter = new DbDocumenter(dbDocumenterConfig);
      }

      @Test
      void itWorksCorrectlyForMultipleSqlAgnosticSchemas() throws IOException, SQLException {
        final var expected =
            Files.readString(
                Path.of(
                    "src/test/resources/postgresql/sql-agnostic-multiple-schema-postgres-expected-output.puml"));
        final var expectedLines = expected.lines().toList();

        final var result = dbDocumenter.generatePuml();
        comparePumlLineByLine(result, expectedLines);
      }
    }

    @Nested
    class GeneratePumlMultipleSchemaSchemaPostgresqlSpecificSqlTests {

      @BeforeAll
      static void containerSetUp() throws SQLException, IOException {
        POSTGRES_TEST_ENVIRONMENT.startContainer();
        connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
        POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(
            connection, "/postgresql/multiple-schema/postgresql-specific-multiple-schema.sql");
      }

      @AfterAll
      static void containerClearDown() throws SQLException {
        connection.close();
        POSTGRES_TEST_ENVIRONMENT.stop();
      }

      @BeforeEach
      void setUp() {
        final var container = POSTGRES_TEST_ENVIRONMENT.getContainer();
        final var dbDocumenterConfig =
            DbDocumenterConfig.builder()
                .databaseName(container.getDatabaseName())
                .username(container.getUsername())
                .schemas(List.of("auth", "core", "analytics", "audit"))
                .password(container.getPassword())
                .databasePort(container.getFirstMappedPort())
                .databaseHost(container.getHost())
                .useSsl(false)
                .build();

        dbDocumenter = new DbDocumenter(dbDocumenterConfig);
      }

      @Test
      void itWorksCorrectlyForMultiplePostgresSchemas() throws IOException, SQLException {
        final var expected =
            Files.readString(
                Path.of(
                    "src/test/resources/postgresql/multiple-schema/postgresql-specific-multiple-schema-expected-output.puml"));
        final var expectedLines = expected.lines().toList();

        final var result = dbDocumenter.generatePuml();
        comparePumlLineByLine(result, expectedLines);
      }
    }
  }
}
