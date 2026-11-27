package dev.shanelucy.dbdocumenter.testhelpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A PostgreSQL Testcontainers environment.
 *
 * <p>The image defaults to {@value #DEFAULT_IMAGE}, the oldest supported major version pinned to a
 * minor release, and can be overridden for a whole run with the {@value #IMAGE_PROPERTY} system
 * property (for example {@code -Ddb-documenter.test.postgres.image=postgres:18}), or per instance
 * with {@link #PostgresTestEnvironment(String)}.
 */
public class PostgresTestEnvironment extends DatabaseTestEnvironment<PostgreSQLContainer> {

  /** The system property that overrides the default image for every no-argument instance. */
  public static final String IMAGE_PROPERTY = "db-documenter.test.postgres.image";

  /** The image used when {@value #IMAGE_PROPERTY} is unset or blank. */
  public static final String DEFAULT_IMAGE = "postgres:15.20";

  /** The newest supported PostgreSQL release, pinned to a minor version like the default. */
  public static final String LATEST_SUPPORTED_IMAGE = "postgres:18.7";

  private final String image;

  /** Creates an environment using the configured image ({@link #configuredImage()}). */
  public PostgresTestEnvironment() {
    this(configuredImage());
  }

  /**
   * Creates an environment using the given image, ignoring {@value #IMAGE_PROPERTY}.
   *
   * @param image a Docker image name compatible with {@code postgres}, such as {@code postgres:18}
   */
  public PostgresTestEnvironment(final String image) {
    if (image.isBlank()) {
      throw new IllegalArgumentException("image must not be blank");
    }
    this.image = image;
  }

  /**
   * Returns the image named by {@value #IMAGE_PROPERTY}, or {@value #DEFAULT_IMAGE} when it is
   * unset or blank.
   */
  public static String configuredImage() {
    final @Nullable String configured = System.getProperty(IMAGE_PROPERTY);
    return configured == null || configured.isBlank() ? DEFAULT_IMAGE : configured.strip();
  }

  /** Returns the image this environment starts. */
  public String getImage() {
    return image;
  }

  @Override
  protected PostgreSQLContainer createContainer(final @Nullable Network network) {
    final var postgresContainer =
        new PostgreSQLContainer(image)
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    if (network != null) {
      postgresContainer.withNetwork(network).withNetworkAliases("db");
    }
    return postgresContainer;
  }

  @Override
  public void initialiseDatabase(final Connection connection, final String sqlResourcePath)
      throws SQLException, IOException {
    try (InputStream is = getClass().getResourceAsStream(sqlResourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("SQL resource not found: " + sqlResourcePath);
      }

      final String sql =
          new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));

      try (final var statement = connection.createStatement()) {
        statement.execute(sql);
      }
    }
  }
}
