package db.documenter.testhelpers;

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

public class PostgresTestEnvironment extends DatabaseTestEnvironment<PostgreSQLContainer> {

  @Override
  protected PostgreSQLContainer createContainer(final @Nullable Network network) {
    final var postgresContainer =
        new PostgreSQLContainer("postgres:15.3")
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
