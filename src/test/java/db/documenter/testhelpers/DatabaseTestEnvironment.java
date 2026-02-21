package db.documenter.testhelpers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;

public abstract class DatabaseTestEnvironment<T extends JdbcDatabaseContainer<T>> {

  protected T container;
  protected Connection connection;
  protected String sqlResourcePath;

  public void startContainer(final Network network) throws SQLException {
    container = createContainer(network);
    container.start();
    connection = container.createConnection("");
  }

  protected void initialiseDatabase() {}

  public void stop() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
    if (container != null) {
      container.stop();
    }
  }

  public void clearDown() {
    // no op
  }

  public T getContainer() {
    return this.container;
  }

  protected abstract T createContainer(final Network network);

  protected void initialiseDatabase(final Connection connection, final String sqlResourcePath)
      throws SQLException, IOException {
    // no op
  }

  public Connection getConnection() {
    return connection;
  }
}
