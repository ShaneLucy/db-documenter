package dev.shanelucy.dbdocumenter.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.DiagramResult;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
    void whenHostIsMissingReturnsInvalidInputExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--database", "mydb",
                  "--username", "user",
                  "--password", "pass",
                  "--schemas", "public");

      assertEquals(1, exitCode);
    }

    @Test
    void whenDatabaseIsMissingReturnsInvalidInputExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host", "localhost",
                  "--username", "user",
                  "--password", "pass",
                  "--schemas", "public");

      assertEquals(1, exitCode);
    }

    @Test
    void whenUsernameIsMissingReturnsInvalidInputExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host", "localhost",
                  "--database", "mydb",
                  "--password", "pass",
                  "--schemas", "public");

      assertEquals(1, exitCode);
    }

    @Test
    void whenSchemaIsMissingReturnsInvalidInputExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host", "localhost",
                  "--database", "mydb",
                  "--username", "user",
                  "--password", "pass");

      assertEquals(1, exitCode);
    }
  }

  @Nested
  class InvalidOptionValueTests {

    @Test
    void whenPortIsNotANumberReturnsInvalidInputExitCode() {
      final var exitCode =
          new CommandLine(new Main())
              .execute(
                  "--host",
                  "localhost",
                  "--port=abc",
                  "--database",
                  "mydb",
                  "--username",
                  "user",
                  "--password",
                  "pass",
                  "--schemas",
                  "public");

      assertEquals(1, exitCode);
    }

    @Test
    void whenPortIsOutOfRangeReturnsInvalidInputExitCodeWithMessage() {
      final var captured =
          captureStderr(
              () ->
                  new CommandLine(new Main())
                      .execute(
                          "--host",
                          "localhost",
                          "--port=70000",
                          "--database",
                          "mydb",
                          "--username",
                          "user",
                          "--password",
                          "pass",
                          "--schemas",
                          "public"));

      assertEquals(1, captured.exitCode());
      assertTrue(
          captured
              .stderr()
              .contains("Configuration error: databasePort must be between 1 and 65535"));
    }
  }

  @Nested
  class OutputHandlingTests {

    private static final String[] BASE_ARGS = {
      "--host", "localhost",
      "--database", "mydb",
      "--username", "user",
      "--password", "pass",
      "--schemas", "public",
      "--ssl=false"
    };

    private int execute(final Main main, final Path erdFile) {
      final var args = new ArrayList<>(List.of(BASE_ARGS));
      args.add("--output");
      args.add(erdFile.toString());
      return new CommandLine(main).execute(args.toArray(String[]::new));
    }

    private Main mainReturning(final DiagramResult result) {
      return new Main(config -> result);
    }

    private DiagramResult erdOnly() {
      return DiagramResult.builder().erdDiagram("@startuml\n@enduml\n").build();
    }

    @Test
    void itLeavesNonDbDocumenterEnumFileInPlaceAndWarns(@TempDir final Path tempDir)
        throws IOException {
      final var erdFile = tempDir.resolve("diagram.puml");
      final var userFile = tempDir.resolve("diagram_enums.puml");
      Files.writeString(userFile, "my own notes");

      final var captured = captureStderr(() -> execute(mainReturning(erdOnly()), erdFile));

      assertEquals(0, captured.exitCode());
      assertEquals("my own notes", Files.readString(userFile));
      assertEquals(
          "Warning: "
              + userFile
              + " exists but was not written by db-documenter; leaving it in place"
              + System.lineSeparator(),
          captured.stderr());
    }

    @Test
    void itDeletesStaleDbDocumenterEnumFileWithoutWarning(@TempDir final Path tempDir)
        throws IOException {
      final var erdFile = tempDir.resolve("diagram.puml");
      final var staleFile = tempDir.resolve("diagram_enums.puml");
      Files.writeString(staleFile, "\n\n@startuml\npackage \"public\" {}\n@enduml\n");

      final var captured = captureStderr(() -> execute(mainReturning(erdOnly()), erdFile));

      assertEquals(0, captured.exitCode());
      assertFalse(Files.exists(staleFile));
      assertEquals("", captured.stderr());
    }

    @Test
    void itDoesNotFailWhenADirectoryExistsAtTheEnumPath(@TempDir final Path tempDir)
        throws IOException {
      final var erdFile = tempDir.resolve("diagram.puml");
      final var directory = Files.createDirectory(tempDir.resolve("diagram_enums.puml"));

      final var captured = captureStderr(() -> execute(mainReturning(erdOnly()), erdFile));

      assertEquals(0, captured.exitCode());
      assertTrue(Files.isDirectory(directory));
      assertEquals(
          "Warning: "
              + directory
              + " exists but was not written by db-documenter; leaving it in place"
              + System.lineSeparator(),
          captured.stderr());
    }

    @Test
    void itReportsReadableMessageWhenParentDirectoryDoesNotExist(@TempDir final Path tempDir) {
      final var erdFile = tempDir.resolve("missing-dir").resolve("diagram.puml");

      final var captured = captureStderr(() -> execute(mainReturning(erdOnly()), erdFile));

      assertEquals(3, captured.exitCode());
      assertEquals(
          "Output error: Failed to write "
              + erdFile
              + ": parent directory does not exist"
              + System.lineSeparator(),
          captured.stderr());
    }

    @Test
    void itReturnsUnexpectedErrorExitCodeWhenValidationFailsWhileGenerating(
        @TempDir final Path tempDir) {
      final var main =
          new Main(
              config -> {
                throw new ValidationException("tableName must not be blank");
              });

      final var captured = captureStderr(() -> execute(main, tempDir.resolve("diagram.puml")));

      assertEquals(3, captured.exitCode());
      assertEquals(
          "Unexpected error: tableName must not be blank" + System.lineSeparator(),
          captured.stderr());
    }

    @Test
    void itReturnsDatabaseErrorExitCodeWhenGenerationFailsWithSqlException(
        @TempDir final Path tempDir) {
      final var main =
          new Main(
              config -> {
                throw new SQLException("connection refused");
              });

      final var captured = captureStderr(() -> execute(main, tempDir.resolve("diagram.puml")));

      assertEquals(2, captured.exitCode());
      assertTrue(captured.stderr().startsWith("Database error: connection refused"));
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
