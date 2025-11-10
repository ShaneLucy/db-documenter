package db.documenter.internal.test.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.testcontainers.postgresql.PostgreSQLContainer;

public class PostgresTestEnvironment extends DatabaseTestEnvironment<PostgreSQLContainer> {

  @Override
  protected PostgreSQLContainer createContainer() {
    return new PostgreSQLContainer("postgres:15.3")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
  }

  @Override
  public void initialiseDatabase(final Connection connection, final String sqlResourcePath)
      throws SQLException, IOException {
    try (InputStream is = getClass().getResourceAsStream(sqlResourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("SQL resource not found: " + sqlResourcePath);
      }

      // Read entire file into a single string
      final String sql =
          new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));

      try (final var statement = connection.createStatement()) {
        statement.execute(sql);
      }
    }
  }
}
