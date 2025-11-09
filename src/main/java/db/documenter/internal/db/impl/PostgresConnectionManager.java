package db.documenter.internal.db.impl;

import db.documenter.DbDocumenterConfig;
import db.documenter.internal.db.api.ConnectionManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PostgresConnectionManager implements ConnectionManager {

  private static final String BASE_CONNECTION_STRING = "jdbc:postgresql://%s:%s/%s";
  private final String url;
  private final DbDocumenterConfig dbDocumenterConfig;
  private final Properties properties;

  public PostgresConnectionManager(
      final DbDocumenterConfig dbDocumenterConfig, final Properties properties) {
    this.dbDocumenterConfig = dbDocumenterConfig;
    this.url =
        String.format(
            BASE_CONNECTION_STRING,
            dbDocumenterConfig.databaseHost(),
            dbDocumenterConfig.databasePort(),
            dbDocumenterConfig.databaseName());
    this.properties = properties;
    setDbProperties();
  }

  public Connection getConnection() throws SQLException {
    final var connection = DriverManager.getConnection(url, properties);

    if (connection != null) {
      System.out.println("Connected to database #1");
    }
    return connection;
  }

  private void setDbProperties() {
    properties.setProperty("user", dbDocumenterConfig.username());
    properties.setProperty("password", dbDocumenterConfig.password());
    properties.setProperty("ssl", String.valueOf(dbDocumenterConfig.useSSL()));
  }
}
