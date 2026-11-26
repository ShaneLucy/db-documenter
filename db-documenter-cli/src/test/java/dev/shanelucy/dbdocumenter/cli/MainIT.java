package dev.shanelucy.dbdocumenter.cli;

import static dev.shanelucy.dbdocumenter.testhelpers.PumlComparison.comparePumlLineByLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.testhelpers.PostgresTestEnvironment;
import dev.shanelucy.dbdocumenter.testhelpers.TestResources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Docker-backed end-to-end tests of the CLI. Kept apart from {@link MainTest} so surefire stays
 * Docker-free while failsafe runs these.
 */
class MainIT {

  @Nested
  class IntegrationTests {

    private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
        new PostgresTestEnvironment();
    private static final String SCHEMAS = "ecommerce,inventory,analytics";
    private static final String EXPECTED_PUML_PATH =
        "/postgresql/sql-agnostic-multiple-schema-postgres-expected-output.puml";

    @BeforeAll
    static void startContainer() throws SQLException, IOException {
      POSTGRES_TEST_ENVIRONMENT.startContainer(null);
      POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(
          POSTGRES_TEST_ENVIRONMENT.getConnection(), "/sql-agnostic-multiple-schema.sql");
    }

    @AfterAll
    static void stopContainer() throws SQLException {
      POSTGRES_TEST_ENVIRONMENT.stop();
    }

    @Test
    void itWritesPumlToStdoutWhenNoOutputOptionProvided() throws IOException {
      final var container = POSTGRES_TEST_ENVIRONMENT.getContainer();
      final var capturedOut = new ByteArrayOutputStream();
      final var originalOut = System.out;
      System.setOut(new PrintStream(capturedOut));

      final int exitCode;
      try {
        exitCode =
            new CommandLine(new Main())
                .execute(
                    "--host",
                    container.getHost(),
                    "--port",
                    String.valueOf(container.getFirstMappedPort()),
                    "--database",
                    container.getDatabaseName(),
                    "--username",
                    container.getUsername(),
                    "--password",
                    container.getPassword(),
                    "--schemas",
                    SCHEMAS,
                    "--ssl=false");
      } finally {
        System.setOut(originalOut);
      }

      assertEquals(0, exitCode);
      final var expected = TestResources.readString(EXPECTED_PUML_PATH);

      comparePumlLineByLine(capturedOut.toString(), expected.lines().toList());
    }

    @Test
    void itWritesPumlToFileWhenOutputOptionIsProvided(@TempDir final Path tempDir)
        throws IOException {
      final var container = POSTGRES_TEST_ENVIRONMENT.getContainer();
      final var outputFile = tempDir.resolve("diagram.puml");

      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host",
                  container.getHost(),
                  "--port",
                  String.valueOf(container.getFirstMappedPort()),
                  "--database",
                  container.getDatabaseName(),
                  "--username",
                  container.getUsername(),
                  "--password",
                  container.getPassword(),
                  "--schemas",
                  SCHEMAS,
                  "--ssl=false",
                  "--output",
                  outputFile.toString());

      assertEquals(0, exitCode);
      final var expected = TestResources.readString(EXPECTED_PUML_PATH);

      comparePumlLineByLine(Files.readString(outputFile), expected.lines().toList());
    }
  }

  @Nested
  class EnumOutputTests {

    private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
        new PostgresTestEnvironment();
    private static final String FIXTURE_DIR = "/postgresql/single-schema/";
    private static final String EXPECTED_ERD =
        FIXTURE_DIR + "postgresql-specific-single-schema-expected-output.puml";
    private static final String EXPECTED_ENUMS =
        FIXTURE_DIR + "postgresql-specific-single-schema-expected-enums-output.puml";

    @BeforeAll
    static void startContainer() throws SQLException, IOException {
      POSTGRES_TEST_ENVIRONMENT.startContainer(null);
      POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(
          POSTGRES_TEST_ENVIRONMENT.getConnection(),
          "/postgresql/single-schema/postgresql-specific-single-schema.sql");
    }

    @AfterAll
    static void stopContainer() throws SQLException {
      POSTGRES_TEST_ENVIRONMENT.stop();
    }

    private String[] args(final String... extra) {
      final var container = POSTGRES_TEST_ENVIRONMENT.getContainer();
      final var base =
          List.of(
              "--host",
              container.getHost(),
              "--port",
              String.valueOf(container.getFirstMappedPort()),
              "--database",
              container.getDatabaseName(),
              "--username",
              container.getUsername(),
              "--password",
              container.getPassword(),
              "--schemas",
              "public",
              "--ssl=false");
      return Stream.concat(base.stream(), Stream.of(extra)).toArray(String[]::new);
    }

    @Test
    void itWritesBothFilesWhenEnumsExist(@TempDir final Path tempDir) throws IOException {
      final var erdFile = tempDir.resolve("diagram.puml");

      final var exitCode =
          new CommandLine(new Main()).execute(args("--output", erdFile.toString()));

      assertEquals(0, exitCode);
      comparePumlLineByLine(
          Files.readString(erdFile), TestResources.readString(EXPECTED_ERD).lines().toList());
      comparePumlLineByLine(
          Files.readString(tempDir.resolve("diagram_enums.puml")),
          TestResources.readString(EXPECTED_ENUMS).lines().toList());
    }

    @Test
    void itWritesOnlyErdToStdoutAndWarnsOnStderrWhenEnumsExist() throws IOException {
      final var capturedOut = new ByteArrayOutputStream();
      final var originalOut = System.out;
      System.setOut(new PrintStream(capturedOut));

      final StderrCapture captured;
      try {
        captured = captureStderr(() -> new CommandLine(new Main()).execute(args()));
      } finally {
        System.setOut(originalOut);
      }

      assertEquals(0, captured.exitCode());
      comparePumlLineByLine(
          capturedOut.toString(), TestResources.readString(EXPECTED_ERD).lines().toList());
      assertEquals(
          "Warning: enum diagram was not written; use --output to write it to a file."
              + System.lineSeparator(),
          captured.stderr());
    }

    @Test
    void itReturnsOutputErrorExitCodeWhenOutputDirectoryDoesNotExist(@TempDir final Path tempDir) {
      final var erdFile = tempDir.resolve("missing-dir").resolve("diagram.puml");

      final var captured =
          captureStderr(
              () -> new CommandLine(new Main()).execute(args("--output", erdFile.toString())));

      assertEquals(3, captured.exitCode());
      assertTrue(captured.stderr().startsWith("Output error: Failed to write " + erdFile));
    }
  }

  @Nested
  class StaleEnumFileTests {

    private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
        new PostgresTestEnvironment();

    @BeforeAll
    static void startContainer() throws SQLException, IOException {
      POSTGRES_TEST_ENVIRONMENT.startContainer(null);
      POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(
          POSTGRES_TEST_ENVIRONMENT.getConnection(), "/sql-agnostic-multiple-schema.sql");
    }

    @AfterAll
    static void stopContainer() throws SQLException {
      POSTGRES_TEST_ENVIRONMENT.stop();
    }

    @Test
    void itDeletesStaleEnumFileAndWritesErdWhenNoEnumsExist(@TempDir final Path tempDir)
        throws IOException {
      final var container = POSTGRES_TEST_ENVIRONMENT.getContainer();
      final var erdFile = tempDir.resolve("diagram.puml");
      final var staleEnumFile = tempDir.resolve("diagram_enums.puml");
      Files.writeString(staleEnumFile, "@startuml\nstale\n@enduml\n");

      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host",
                  container.getHost(),
                  "--port",
                  String.valueOf(container.getFirstMappedPort()),
                  "--database",
                  container.getDatabaseName(),
                  "--username",
                  container.getUsername(),
                  "--password",
                  container.getPassword(),
                  "--schemas",
                  "ecommerce,inventory,analytics",
                  "--ssl=false",
                  "--output",
                  erdFile.toString());

      assertEquals(0, exitCode);
      assertTrue(Files.exists(erdFile));
      assertFalse(Files.exists(staleEnumFile));
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private record StderrCapture(int exitCode, String stderr) {}

  private StderrCapture captureStderr(final java.util.function.IntSupplier action) {
    final var captured = new ByteArrayOutputStream();
    final var originalErr = System.err;
    System.setErr(new PrintStream(captured));
    final int code;
    try {
      code = action.getAsInt();
    } finally {
      System.setErr(originalErr);
    }
    return new StderrCapture(code, captured.toString());
  }
}
