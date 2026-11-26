package db.documenter.cli;

import static db.documenter.testhelpers.PumlComparison.comparePumlLineByLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.testhelpers.PostgresTestEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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

class MainTest {

  @Nested
  class VersionTests {

    @Test
    void itExitsZeroAndPrintsDevelopmentBuildVersion() {
      final var out = new StringWriter();
      final var commandLine = new CommandLine(new Main());
      commandLine.setOut(new PrintWriter(out, true));

      final var exitCode = commandLine.execute("--version");

      assertEquals(0, exitCode);
      assertEquals(
          ManifestVersionProvider.DEVELOPMENT_VERSION + System.lineSeparator(), out.toString());
    }
  }

  @Nested
  class ArgumentParsingTests {

    @Test
    void whenHostIsMissingReturnsNonZeroExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--database", "mydb",
                  "--username", "user",
                  "--password", "pass",
                  "--schemas", "public");

      assertNotEquals(0, exitCode);
    }

    @Test
    void whenDatabaseIsMissingReturnsNonZeroExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host", "localhost",
                  "--username", "user",
                  "--password", "pass",
                  "--schemas", "public");

      assertNotEquals(0, exitCode);
    }

    @Test
    void whenUsernameIsMissingReturnsNonZeroExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host", "localhost",
                  "--database", "mydb",
                  "--password", "pass",
                  "--schemas", "public");

      assertNotEquals(0, exitCode);
    }

    @Test
    void whenSchemaIsMissingReturnsNonZeroExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host", "localhost",
                  "--database", "mydb",
                  "--username", "user",
                  "--password", "pass");

      assertNotEquals(0, exitCode);
    }
  }

  @Nested
  class ValidationErrorTests {

    @Test
    void whenHostIsBlankWhitespaceReturnsValidationExitCode() {
      final var captured = captureStderr(() -> executeWith("--host", " "));

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
    }

    @Test
    void whenDatabaseIsBlankWhitespaceReturnsValidationExitCode() {
      final var captured = captureStderr(this::executeWithBlankDatabase);

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
    }

    @Test
    void whenUsernameIsBlankWhitespaceReturnsValidationExitCode() {
      final var captured = captureStderr(this::executeWithBlankUsername);

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
    }

    @Test
    void whenPasswordIsBlankWhitespaceReturnsValidationExitCode() {
      final var captured = captureStderr(this::executeWithBlankPassword);

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
    }

    @Test
    void whenPasswordOptionIsAbsentReturnsValidationExitCodeWithMessage() {
      final var captured =
          captureStderr(
              () ->
                  new CommandLine(new Main())
                      .execute(
                          "--host",
                          "localhost",
                          "--database",
                          "mydb",
                          "--username",
                          "user",
                          "--schemas",
                          "public",
                          "--ssl=false"));

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error: password must not be blank"));
    }

    private int executeWith(final String nameArg, final String valueArg) {
      return new CommandLine(new Main())
          .execute(
              nameArg,
              valueArg,
              "--database",
              "mydb",
              "--username",
              "user",
              "--password",
              "pass",
              "--schemas",
              "public",
              "--ssl=false");
    }

    private int executeWithBlankDatabase() {
      return new CommandLine(new Main())
          .execute(
              "--host",
              "localhost",
              "--database",
              " ",
              "--username",
              "user",
              "--password",
              "pass",
              "--schemas",
              "public",
              "--ssl=false");
    }

    private int executeWithBlankUsername() {
      return new CommandLine(new Main())
          .execute(
              "--host",
              "localhost",
              "--database",
              "mydb",
              "--username",
              " ",
              "--password",
              "pass",
              "--schemas",
              "public",
              "--ssl=false");
    }

    private int executeWithBlankPassword() {
      return new CommandLine(new Main())
          .execute(
              "--host",
              "localhost",
              "--database",
              "mydb",
              "--username",
              "user",
              "--password",
              " ",
              "--schemas",
              "public",
              "--ssl=false");
    }
  }

  @Nested
  class DatabaseConnectivityTests {

    @Test
    void whenConnectionIsRefusedReturnsDatabaseErrorExitCode() {
      // Port 1 on localhost is closed and responds with connection refused immediately,
      // avoiding any DNS timeout from an unresolvable hostname.
      final var captured =
          captureStderr(
              () ->
                  new CommandLine(new Main())
                      .execute(
                          "--host",
                          "localhost",
                          "--port",
                          "1",
                          "--database",
                          "mydb",
                          "--username",
                          "user",
                          "--password",
                          "pass",
                          "--schemas",
                          "public",
                          "--ssl=false"));

      assertEquals(2, captured.exitCode());
      assertTrue(captured.stderr().contains("Database error"));
    }
  }

  @Nested
  class IntegrationTests {

    private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
        new PostgresTestEnvironment();
    private static final String SCHEMAS = "ecommerce,inventory,analytics";
    private static final String EXPECTED_PUML_PATH =
        "src/test/resources/postgresql/sql-agnostic-multiple-schema-postgres-expected-output.puml";

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
      final var expected = Files.readString(Path.of(EXPECTED_PUML_PATH));

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
      final var expected = Files.readString(Path.of(EXPECTED_PUML_PATH));

      comparePumlLineByLine(Files.readString(outputFile), expected.lines().toList());
    }
  }

  @Nested
  class EnumOutputTests {

    private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
        new PostgresTestEnvironment();
    private static final String FIXTURE_DIR = "src/test/resources/postgresql/single-schema/";
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
          Files.readString(erdFile), Files.readString(Path.of(EXPECTED_ERD)).lines().toList());
      comparePumlLineByLine(
          Files.readString(tempDir.resolve("diagram_enums.puml")),
          Files.readString(Path.of(EXPECTED_ENUMS)).lines().toList());
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
          capturedOut.toString(), Files.readString(Path.of(EXPECTED_ERD)).lines().toList());
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
      Files.writeString(staleEnumFile, "stale");

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
