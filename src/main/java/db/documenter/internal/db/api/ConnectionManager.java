package db.documenter.internal.db.api;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionManager {

  Connection getConnection() throws SQLException;
}
