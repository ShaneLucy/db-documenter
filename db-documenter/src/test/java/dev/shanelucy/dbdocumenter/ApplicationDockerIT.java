package dev.shanelucy.dbdocumenter;

import static dev.shanelucy.dbdocumenter.testhelpers.PumlComparison.comparePumlLineByLine;

import dev.shanelucy.dbdocumenter.testhelpers.PostgresTestEnvironment;
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
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

class ApplicationDockerIT {

  /** Failsafe runs with the module as working directory; the Dockerfile lives at the repo root. */
  private static final Path DOCKERFILE =
      Path.of(System.getProperty("dbdocumenter.repoRoot", ".."), "Dockerfile");

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
              .withFileFromPath("Dockerfile", DOCKERFILE)
              .withFileFromPath("db-documenter/target/" + fatJar.getFileName(), fatJar);

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

    @Test
    void itWritesErdAndEnumFilesToBindMountedDirectoryViaDockerImage(@TempDir final Path outputDir)
        throws Exception {
      final var enumNetwork = Network.newNetwork();
      final var enumEnvironment = new PostgresTestEnvironment();
      try {
        enumEnvironment.startContainer(enumNetwork);
        enumEnvironment.initialiseDatabase(
            enumEnvironment.getConnection(),
            "/postgresql/single-schema/postgresql-specific-single-schema.sql");

        final var fatJar = findFatJar();
        final var appImage =
            new ImageFromDockerfile()
                .withFileFromPath("Dockerfile", DOCKERFILE)
                .withFileFromPath("db-documenter/target/" + fatJar.getFileName(), fatJar);

        // The image runs as non-root 'appuser', which cannot write to a host-owned bind mount.
        // Run the container as the host uid:gid so the mount is writable and the files are
        // owned by the invoking user.
        final var hostUser = hostUidGid();

        try (final var appContainer =
            new GenericContainer<>(appImage)
                .withNetwork(enumNetwork)
                .withCreateContainerCmdModifier(cmd -> cmd.withUser(hostUser))
                .withFileSystemBind(outputDir.toString(), "/out", BindMode.READ_WRITE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
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
                    "public",
                    "--ssl=false",
                    "--output",
                    "/out/diagram.puml")) {
          appContainer.start();
        }

        comparePumlLineByLine(
            Files.readString(outputDir.resolve("diagram.puml")),
            Files.readString(
                    Path.of(
                        "src/test/resources/postgresql/single-schema/postgresql-specific-single-schema-expected-output.puml"))
                .lines()
                .toList());
        comparePumlLineByLine(
            Files.readString(outputDir.resolve("diagram_enums.puml")),
            Files.readString(
                    Path.of(
                        "src/test/resources/postgresql/single-schema/postgresql-specific-single-schema-expected-enums-output.puml"))
                .lines()
                .toList());
      } finally {
        enumEnvironment.stop();
        enumNetwork.close();
      }
    }

    private static Path findFatJar() throws IOException {
      try (final Stream<Path> files = Files.list(Path.of("target"))) {
        return files
            .filter(p -> p.getFileName().toString().endsWith("-jar-with-dependencies.jar"))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Fat JAR not found in target/. Run 'mvn package' first."));
      }
    }

    private static String hostUidGid() throws IOException {
      final var uid = (Integer) Files.getAttribute(Path.of("target"), "unix:uid");
      final var gid = (Integer) Files.getAttribute(Path.of("target"), "unix:gid");
      return uid + ":" + gid;
    }
  }
}
