package db.documenter;

import static org.junit.jupiter.api.Assertions.fail;

import db.documenter.internal.test.helpers.PostgresTestEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.*;

class DbDocumenterTest {

  private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
      new PostgresTestEnvironment();
  private DbDocumenter dbDocumenter;
  private static Connection connection;

  @BeforeAll
  static void containerSetUp() throws SQLException, IOException {
    POSTGRES_TEST_ENVIRONMENT.startContainer();
    connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
    POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(connection, "/single-schema/test-db.sql");
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

  @Nested
  class GeneratePuml {

    @Test
    void itWorksCorrectlyForASingleSchema() throws IOException, SQLException {
      final var expected =
          Files.readString(Path.of("src/test/resources/single-schema/test-puml.puml"));
      final var expectedLines = expected.lines().toList();

      final var result = dbDocumenter.generatePuml();
      final var resultLines = result.lines().toList();

      int max = Math.max(resultLines.size(), expectedLines.size());

      for (int i = 0; i < max; i++) {
        String resultLine = (i < resultLines.size()) ? resultLines.get(i) : "<missing>";
        String expectedLine = (i < expectedLines.size()) ? expectedLines.get(i) : "<missing>";

        if (!Objects.equals(resultLine, expectedLine)) {
          fail(
              """
                    PUML mismatch at line %d:
                    Expected: %s
                    Actual  : %s
                    """
                  .formatted(i + 1, expectedLine, resultLine));
        }
      }
    }
  }
}
