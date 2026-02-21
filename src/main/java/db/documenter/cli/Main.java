package db.documenter.cli;

import db.documenter.DbDocumenter;
import db.documenter.DbDocumenterConfig;
import db.documenter.internal.exceptions.ValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI entry point for the db-documenter tool.
 *
 * <p>Parses command-line arguments via picocli, constructs a {@link DbDocumenterConfig}, delegates
 * to {@link DbDocumenter#generatePuml()}, and routes the resulting PlantUML output either to a file
 * (when {@code --output} is specified) or to stdout.
 *
 * <p>Interactive password prompting is supported: omitting the {@code --password} value causes
 * picocli to prompt the user securely at runtime, keeping credentials out of shell history.
 *
 * <p><b>Exit codes:</b>
 *
 * <ul>
 *   <li>{@code 0} - success
 *   <li>{@code 1} - configuration or validation error
 *   <li>{@code 2} - database connectivity error
 *   <li>{@code 3} - unexpected error (I/O or other)
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
    version = "1.0.22")
public final class Main implements Callable<Integer> {
  private static final int EXIT_SUCCESS = 0;

  private static final int EXIT_VALIDATION_ERROR = 1;

  private static final int EXIT_DATABASE_ERROR = 2;

  private static final int EXIT_UNEXPECTED_ERROR = 3;

  // Picocli populates these fields after construction, so they must not carry @NonNull.
  // --password and --output are intentionally nullable: null signals "not provided".

  @Option(names = "--host", description = "Database hostname.", required = true)
  private String host;

  @Option(
      names = "--port",
      description = "Database port (default: ${DEFAULT-VALUE}).",
      defaultValue = "5432")
  private int port;

  @Option(names = "--database", description = "Database name.", required = true)
  private String database;

  @Option(names = "--username", description = "Database username.", required = true)
  private String username;

  @Option(
      names = "--password",
      description = "Database password. Omit the value to be prompted interactively.",
      interactive = true,
      arity = "0..1")
  private String password;

  @Option(
      names = "--schemas",
      description = "Comma-separated list of schemas to document.",
      required = true,
      split = ",")
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

  @Option(names = "--output", description = "Output file path. Omit to write to stdout.")
  private String output;

  /**
   * Executes the documentation generation pipeline.
   *
   * <p>Builds a {@link DbDocumenterConfig} from the parsed CLI options, invokes {@link
   * DbDocumenter#generatePuml()}, and writes the result to the configured output destination.
   * Catches domain-specific exceptions and maps them to the appropriate integer exit codes so that
   * callers and shell scripts can react to specific failure categories.
   *
   * @return {@code 0} on success; {@code 1} for validation errors; {@code 2} for database errors;
   *     {@code 3} for unexpected errors
   */
  // Suppressing SystemPrintln & AvoidCatchingGenericExceptio
  // are acceptable here since this is the CLI entrypoint to the application
  @SuppressWarnings({"PMD.SystemPrintln", "PMD.AvoidCatchingGenericException"})
  @Override
  public Integer call() {
    try {
      final var config =
          DbDocumenterConfig.builder()
              .databaseHost(host)
              .databasePort(port)
              .databaseName(database)
              .username(username)
              .password(password)
              .schemas(schemas)
              .useSsl(ssl)
              .build();

      final var puml = new DbDocumenter(config).generatePuml();

      if (output != null) {
        Files.writeString(Path.of(output), puml);
      } else {
        System.out.print(puml);
      }

      return EXIT_SUCCESS;

    } catch (final ValidationException e) {
      System.err.println("Configuration error: " + e.getMessage());
      System.err.println("Use --help for usage information.");
      return EXIT_VALIDATION_ERROR;

    } catch (final SQLException e) {
      System.err.println("Database error: " + e.getMessage());
      System.err.println("Check connection parameters and database availability.");
      return EXIT_DATABASE_ERROR;

    } catch (final IOException e) {
      System.err.println("Output error: " + e.getMessage());
      return EXIT_UNEXPECTED_ERROR;

    } catch (final Exception e) {
      System.err.println("Unexpected error: " + e.getMessage());
      return EXIT_UNEXPECTED_ERROR;
    }
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
