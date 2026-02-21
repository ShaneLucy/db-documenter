package db.documenter.cli;

import static db.documenter.testhelpers.PumlComparison.comparePumlLineByLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.testhelpers.PostgresTestEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class MainTest {

  @Nested
  class ArgumentParsingTests {

    @Test
    void whenHostIsMissing_returnsNonZeroExitCode() {
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
    void whenDatabaseIsMissing_returnsNonZeroExitCode() {
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
    void whenUsernameIsMissing_returnsNonZeroExitCode() {
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
    void whenSchemaIsMissing_returnsNonZeroExitCode() {
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
    void whenHostIsBlankWhitespace_returnsValidationExitCode() {
      final var captured = captureStderr(() -> executeWith("--host", " "));

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
    }

    @Test
    void whenDatabaseIsBlankWhitespace_returnsValidationExitCode() {
      final var captured = captureStderr(this::executeWithBlankDatabase);

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
    }

    @Test
    void whenUsernameIsBlankWhitespace_returnsValidationExitCode() {
      final var captured = captureStderr(this::executeWithBlankUsername);

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
    }

    @Test
    void whenPasswordIsBlankWhitespace_returnsValidationExitCode() {
      final var captured = captureStderr(this::executeWithBlankPassword);

      assertEquals(1, captured.exitCode());
      assertTrue(captured.stderr().contains("Configuration error"));
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
    void whenConnectionIsRefused_returnsDatabaseErrorExitCode() {
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
