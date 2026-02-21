package db.documenter;

import static db.documenter.testhelpers.PumlComparison.comparePumlLineByLine;

import db.documenter.testhelpers.PostgresTestEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

class ApplicationDockerIT {

  @Nested
  class PostgresqlTest {
    private static final Network NETWORK = Network.newNetwork();

    private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
        new PostgresTestEnvironment();
    private static Connection connection;

    @BeforeAll
    static void containerSetUp() throws SQLException, IOException {
      POSTGRES_TEST_ENVIRONMENT.startContainer(NETWORK);
      connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
      POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(connection, "/sql-agnostic-multiple-schema.sql");
    }

    @AfterAll
    static void containerClearDown() throws SQLException {
      connection.close();
      POSTGRES_TEST_ENVIRONMENT.stop();
      NETWORK.close();
    }

    @Test
    void itGeneratesCorrectPumlOutputForMultipleSchemasViaDockerImage() throws IOException {
      final Path fatJar;
      try (final Stream<Path> files = Files.list(Path.of("target"))) {
        fatJar =
            files
                .filter(p -> p.getFileName().toString().endsWith("-jar-with-dependencies.jar"))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Fat JAR not found in target/. Run 'mvn package' first."));
      }

      final var appImage =
          new ImageFromDockerfile()
              .withFileFromPath("Dockerfile", Path.of("Dockerfile"))
              .withFileFromPath("target/" + fatJar.getFileName(), fatJar);

      final var stdoutCapture = new StringBuilder();

      try (final var appContainer =
          new GenericContainer<>(appImage)
              .withNetwork(NETWORK)
              .withCommand(
                  "--host",
                  "db",
                  "--port",
                  "5432",
                  "--database",
                  "testdb",
                  "--username",
                  "test",
                  "--password",
                  "test",
                  "--schemas",
                  "ecommerce,inventory,analytics",
                  "--ssl=false")
              .waitingFor(Wait.forLogMessage("@enduml\\n", 1))
              .withLogConsumer(
                  frame -> {
                    if (frame.getType() == OutputFrame.OutputType.STDOUT) {
                      stdoutCapture.append(frame.getUtf8String());
                    }
                  })) {

        appContainer.start();

        final var expected =
            Files.readString(
                Path.of(
                    "src/test/resources/postgresql/sql-agnostic-multiple-schema-postgres-expected-output.puml"));

        comparePumlLineByLine(stdoutCapture.toString(), expected.lines().toList());
      }
    }
  }
}
