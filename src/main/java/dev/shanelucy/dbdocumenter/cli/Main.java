package dev.shanelucy.dbdocumenter.cli;

import dev.shanelucy.dbdocumenter.DbDocumenter;
import dev.shanelucy.dbdocumenter.DbDocumenterConfig;
import dev.shanelucy.dbdocumenter.DiagramResult;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.utils.LogUtils;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI entry point for the db-documenter tool.
 *
 * <p>Parses command-line arguments via picocli, constructs a {@link DbDocumenterConfig}, delegates
 * to {@link DbDocumenter#generate()}, and routes the resulting PlantUML output either to files
 * (when {@code --output} is specified) or to stdout.
 *
 * <p><b>Output:</b> with {@code --output <path>} the ERD is written to {@code <path>} and the enum
 * diagram, if any schema defines enums, to a sibling file with {@code _enums} appended to the file
 * name before its extension (e.g. {@code schema.puml} and {@code schema_enums.puml}). When there
 * are no enums, a stale enum file left by a previous run is deleted so the output never mixes runs;
 * a file at that path is only deleted if it starts with {@code @startuml}, otherwise it is left in
 * place and a warning is printed to stderr. Without {@code --output} only the ERD goes to stdout
 * and a warning on stderr notes that the enum diagram was not written.
 *
 * <p>Interactive password prompting is supported: omitting the {@code --password} value causes
 * picocli to prompt the user securely at runtime, keeping credentials out of shell history.
 *
 * <p><b>Exit codes:</b>
 *
 * <ul>
 *   <li>{@code 0} - success
 *   <li>{@code 1} - invalid command-line input or configuration, including missing or malformed
 *       options detected by picocli (overridden from picocli's default of 2)
 *   <li>{@code 2} - database connectivity error
 *   <li>{@code 3} - unexpected error, including a validation failure while generating (which points
 *       at unexpected catalog data, not user input) and failure to write or delete either output
 *       file (if the ERD was written before the enum file failed, the ERD file is left in place)
 * </ul>
 *
 * <p><b>Example usage:</b>
 *
 * <pre>{@code
 * db-documenter \
 *   --host localhost \
 *   --port 5432 \
 *   --database mydb \
 *   --username myuser \
 *   --password secret \
 *   --schemas public,audit \
 *   --output diagram.puml
 * }</pre>
 *
 * @see DbDocumenter
 * @see DbDocumenterConfig
 */
@Command(
    name = "db-documenter",
    description = "Generates a PlantUML entity-relationship diagram from a live database schema.",
    mixinStandardHelpOptions = true,
    // Picocli's default of 2 for bad input would collide with EXIT_DATABASE_ERROR.
    exitCodeOnInvalidInput = 1,
    versionProvider = ManifestVersionProvider.class)
public final class Main implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

  private static final int PREFIX_LENGTH = 256;

  private static final int EXIT_SUCCESS = 0;

  private static final int EXIT_INVALID_INPUT = 1;

  private static final int EXIT_DATABASE_ERROR = 2;

  private static final int EXIT_UNEXPECTED_ERROR = 3;

  private final DiagramGenerator diagramGenerator;

  // Picocli populates required options before call(); --password and --output are optional.

  @Option(names = "--host", description = "Database hostname.", required = true)
  @SuppressWarnings("NullAway.Init") // populated by picocli before call()
  private String host;

  @Option(
      names = "--port",
      description = "Database port (default: ${DEFAULT-VALUE}).",
      defaultValue = "5432")
  private int port;

  @Option(names = "--database", description = "Database name.", required = true)
  @SuppressWarnings("NullAway.Init") // populated by picocli before call()
  private String database;

  @Option(names = "--username", description = "Database username.", required = true)
  @SuppressWarnings("NullAway.Init") // populated by picocli before call()
  private String username;

  @Option(
      names = "--password",
      description = "Database password. Omit the value to be prompted interactively.",
      interactive = true,
      arity = "0..1")
  private @Nullable String password;

  @Option(
      names = "--schemas",
      description = "Comma-separated list of schemas to document.",
      required = true,
      split = ",")
  @SuppressWarnings("NullAway.Init") // populated by picocli before call()
  private List<String> schemas;

  @Option(
      names = "--ssl",
      description =
          "Use SSL for the database connection. Pass --ssl=false to disable (default:"
              + " ${DEFAULT-VALUE}).",
      arity = "0..1",
      fallbackValue = "true",
      defaultValue = "true")
  private boolean ssl;

  @Option(
      names = "--output",
      description =
          "ERD output file path. The enum diagram is written next to it with an _enums suffix"
              + " before the extension. Omit to write the ERD to stdout.")
  private @Nullable String output;

  /** Creates the production command, which generates diagrams with {@link DbDocumenter}. */
  public Main() {
    this(config -> new DbDocumenter(config).generate());
  }

  Main(final DiagramGenerator diagramGenerator) {
    this.diagramGenerator = diagramGenerator;
  }

  /**
   * Executes the documentation generation pipeline.
   *
   * <p>Builds a {@link DbDocumenterConfig} from the parsed CLI options, invokes {@link
   * DbDocumenter#generate()}, and writes the result to the configured output destination. Catches
   * domain-specific exceptions and maps them to the appropriate integer exit codes so that callers
   * and shell scripts can react to specific failure categories.
   *
   * @return {@code 0} on success; {@code 1} for invalid input or configuration; {@code 2} for
   *     database errors; {@code 3} for unexpected errors
   */
  // Suppressing SystemPrintln & AvoidCatchingGenericExceptio
  // are acceptable here since this is the CLI entrypoint to the application
  @SuppressWarnings({"PMD.SystemPrintln", "PMD.AvoidCatchingGenericException"})
  @Override
  public Integer call() {
    final DbDocumenterConfig config;
    try {
      config = buildConfig();
    } catch (final ValidationException e) {
      System.err.println("Configuration error: " + e.getMessage());
      System.err.println("Use --help for usage information.");
      return EXIT_INVALID_INPUT;
    }

    try {
      final var diagrams = diagramGenerator.generate(config);
      writeOutput(diagrams);
      return EXIT_SUCCESS;

    } catch (final SQLException e) {
      System.err.println("Database error: " + e.getMessage());
      System.err.println("Check connection parameters and database availability.");
      return EXIT_DATABASE_ERROR;

    } catch (final IOException e) {
      System.err.println("Output error: " + e.getMessage());
      return EXIT_UNEXPECTED_ERROR;

    } catch (final Exception e) {
      // Includes ValidationException raised while generating: that is bad catalog data, not input.
      System.err.println("Unexpected error: " + e.getMessage());
      return EXIT_UNEXPECTED_ERROR;
    }
  }

  private DbDocumenterConfig buildConfig() {
    return DbDocumenterConfig.builder()
        .databaseHost(host)
        .databasePort(port)
        .databaseName(database)
        .username(username)
        .password(Validators.isNotBlank(password, "password"))
        .schemas(schemas)
        .useSsl(ssl)
        .build();
  }

  @SuppressWarnings("PMD.SystemPrintln") // CLI entrypoint
  private void writeOutput(final DiagramResult diagrams) throws IOException {
    final var outputPath = output;
    if (outputPath != null) {
      writeToFiles(Path.of(outputPath), diagrams);
    } else {
      System.out.print(diagrams.erdDiagram());
      if (diagrams.enumDiagram().isPresent()) {
        System.err.println(
            "Warning: enum diagram was not written; use --output to write it to a file.");
      }
    }
  }

  private static void writeToFiles(final Path erdPath, final DiagramResult diagrams)
      throws IOException {
    writeFile(erdPath, diagrams.erdDiagram());

    final var enumPath = EnumOutputPaths.derive(erdPath);
    final var enumDiagram = diagrams.enumDiagram();
    if (enumDiagram.isPresent()) {
      writeFile(enumPath, enumDiagram.orElseThrow());
    } else {
      deleteStaleFile(enumPath);
    }
  }

  private static void writeFile(final Path path, final String content) throws IOException {
    try {
      Files.writeString(path, content);
    } catch (final IOException e) {
      throw new IOException("Failed to write " + path + ": " + describe(e), e);
    }
  }

  /**
   * Deletes a leftover enum diagram, but only if it looks like one of ours. Anything else at that
   * path (a user's file, a directory) is not ours to remove.
   */
  @SuppressWarnings("PMD.SystemPrintln") // CLI entrypoint
  private static void deleteStaleFile(final Path path) throws IOException {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    if (!isGeneratedDiagram(path)) {
      System.err.println(
          "Warning: " + path + " exists but was not written by db-documenter; leaving it in place");
      return;
    }
    try {
      Files.delete(path);
      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.log(
            Level.FINE,
            "No enums found; deleted stale enum diagram {0}",
            LogUtils.sanitizeForLog(path));
      }
    } catch (final IOException e) {
      throw new IOException("Failed to delete stale file " + path + ": " + describe(e), e);
    }
  }

  private static boolean isGeneratedDiagram(final Path path) {
    if (!Files.isRegularFile(path)) {
      return false;
    }
    // ISO-8859-1 cannot fail to decode, so binary files are simply "not ours". Reading a bounded
    // prefix avoids slurping large files; leading blank lines are skipped by stripLeading.
    try (var reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
      final var buffer = new char[PREFIX_LENGTH];
      final int read = reader.read(buffer);
      return read > 0 && new String(buffer, 0, read).stripLeading().startsWith("@startuml");
    } catch (final IOException e) {
      return false;
    }
  }

  private static String describe(final IOException exception) {
    if (exception instanceof NoSuchFileException) {
      return "parent directory does not exist";
    }
    final var message = exception.getMessage();
    final var name = exception.getClass().getSimpleName();
    return message == null ? name : name + ": " + message;
  }

  /**
   * Application entry point.
   *
   * <p>Delegates argument parsing and command execution entirely to picocli so that {@code --help},
   * {@code --version}, and exit-code propagation are handled consistently.
   *
   * @param args command-line arguments supplied by the shell
   */
  public static void main(final String[] args) {
    System.exit(new CommandLine(new Main()).execute(args));
  }
}
