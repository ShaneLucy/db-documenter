package db.documenter.internal.connection.api;

import java.sql.Connection;
import java.sql.SQLException;

/** A {@link FunctionalInterface} responsible for managing database connections. */
@FunctionalInterface
public interface ConnectionManager {

  /**
   * Establishes a connection with the database.
   *
   * @return the a {@link Connection} connection.
   * @throws SQLException if a connection could not be established
   */
  Connection getConnection() throws SQLException;
}
