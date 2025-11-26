package db.documenter.internal.connection;

import db.documenter.DbDocumenterConfig;
import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.connection.impl.postgresql.PostgresConnectionManager;

public final class ConnectionManagerFactory {

  private final DbDocumenterConfig dbDocumenterConfig;

  public ConnectionManagerFactory(final DbDocumenterConfig dbDocumenterConfig) {
    this.dbDocumenterConfig = dbDocumenterConfig;
  }

  public ConnectionManager createConnectionManager() {
    return switch (dbDocumenterConfig.rdmsType()) {
      case POSTGRESQL -> new PostgresConnectionManager(dbDocumenterConfig);
    };
  }
}
