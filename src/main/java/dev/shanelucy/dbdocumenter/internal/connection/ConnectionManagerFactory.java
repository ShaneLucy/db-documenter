package dev.shanelucy.dbdocumenter.internal.connection;

import dev.shanelucy.dbdocumenter.DbDocumenterConfig;
import dev.shanelucy.dbdocumenter.internal.connection.api.ConnectionManager;
import dev.shanelucy.dbdocumenter.internal.connection.impl.postgresql.PostgresConnectionManager;

public final class ConnectionManagerFactory {

  private final DbDocumenterConfig dbDocumenterConfig;

  public ConnectionManagerFactory(final DbDocumenterConfig dbDocumenterConfig) {
    this.dbDocumenterConfig = dbDocumenterConfig;
  }

  public ConnectionManager createConnectionManager() {
    return switch (dbDocumenterConfig.databaseType()) {
      case POSTGRESQL -> new PostgresConnectionManager(dbDocumenterConfig);
    };
  }
}
