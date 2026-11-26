package dev.shanelucy.dbdocumenter.internal.connection.impl.postgresql;

import dev.shanelucy.dbdocumenter.DbDocumenterConfig;
import dev.shanelucy.dbdocumenter.internal.connection.api.ConnectionManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PostgresConnectionManager implements ConnectionManager {

  private static final Logger LOGGER = Logger.getLogger(PostgresConnectionManager.class.getName());
  private final String url;
  private final DbDocumenterConfig dbDocumenterConfig;
  private final Properties properties;

  public PostgresConnectionManager(final DbDocumenterConfig dbDocumenterConfig) {
    this.dbDocumenterConfig = dbDocumenterConfig;
    this.url =
        String.format(
            "jdbc:postgresql://%s:%s/%s",
            dbDocumenterConfig.databaseHost(),
            dbDocumenterConfig.databasePort(),
            dbDocumenterConfig.databaseName());
    this.properties = new Properties();
    setDbProperties();
  }

  @Override
  public Connection getConnection() throws SQLException {
    final var connection = DriverManager.getConnection(url, properties);

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Connected to postgres database");
    }
    return connection;
  }

  private void setDbProperties() {
    properties.setProperty("user", dbDocumenterConfig.username());
    properties.setProperty("password", dbDocumenterConfig.password());
    properties.setProperty("ssl", String.valueOf(dbDocumenterConfig.useSsl()));
  }
}
