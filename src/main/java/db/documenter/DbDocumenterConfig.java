package db.documenter;

import db.documenter.internal.validation.Validators;
import java.util.List;

public record DbDocumenterConfig(
    List<String> schemas,
    String databaseHost,
    int databasePort,
    String databaseName,
    boolean useSSL,
    String username,
    String password) {

  public DbDocumenterConfig {
    Validators.containsAtLeast1Item(schemas, "schemas");
    Validators.isNotBlank(databaseHost, "databaseHost");
    Validators.isNotBlank(databaseName, "databaseName");
    Validators.isNotBlank(username, "username");
    Validators.isNotBlank(password, "password");
    schemas = List.copyOf(schemas);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<String> schemas;
    private String databaseHost;
    private String databaseName;
    private int databasePort = 5432;
    private boolean useSSL = true;
    private String username;
    private String password;

    public Builder schemas(final List<String> schemas) {
      this.schemas = List.copyOf(schemas);
      return this;
    }

    public Builder databaseHost(final String databaseHost) {
      this.databaseHost = databaseHost;
      return this;
    }

    public Builder databasePort(final int databasePort) {
      this.databasePort = databasePort;
      return this;
    }

    public Builder databaseName(final String databaseName) {
      this.databaseName = databaseName;
      return this;
    }

    public Builder useSSL(final boolean useSSL) {
      this.useSSL = useSSL;
      return this;
    }

    public Builder username(final String username) {
      this.username = username;
      return this;
    }

    public Builder password(final String password) {
      this.password = password;
      return this;
    }

    public DbDocumenterConfig build() {
      validate();
      return new DbDocumenterConfig(
          schemas, databaseHost, databasePort, databaseName, useSSL, username, password);
    }

    private void validate() {
      Validators.containsAtLeast1Item(schemas, "schemas");
      Validators.isNotBlank(databaseHost, "databaseHost");
      Validators.isNotBlank(databaseName, "databaseName");
      Validators.isNotBlank(username, "username");
      Validators.isNotBlank(password, "password");
    }
  }
}
