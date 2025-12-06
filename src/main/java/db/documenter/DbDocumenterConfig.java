package db.documenter;

import db.documenter.internal.validation.Validators;
import java.util.List;

/** Configuration object for {@link DbDocumenter} application. */
public record DbDocumenterConfig(
    List<String> schemas,
    String databaseHost,
    int databasePort,
    String databaseName,
    boolean useSsl,
    String username,
    String password,
    DatabaseType databaseType) {

  /**
   * Validates the required configuration fields.
   *
   * @param schemas A {@link List} of schema names. at least 1 schema must be provided.
   * @param databaseHost The database host name, must not be blank.
   * @param databasePort The database port, defaults to 5432.
   * @param databaseName The database name, must not be blank.
   * @param useSsl If the database connection should use ssl, defaults to TRUE.
   * @param username The username of the user used to connect to the database, must not be blank.
   * @param password The password of the user used to connect to the database, must not be blank.
   * @param databaseType The type of database being used, defaults to Postgresql
   */
  public DbDocumenterConfig {
    Validators.containsAtLeast1Item(schemas, "schemas");
    Validators.isNotBlank(databaseHost, "databaseHost");
    Validators.isNotBlank(databaseName, "databaseName");
    Validators.isNotBlank(username, "username");
    Validators.isNotBlank(password, "password");
    Validators.isNotNull(databaseType, "databaseType");
    schemas = List.copyOf(schemas);
  }

  /** Convenience utility for creating {@link DbDocumenterConfig} object. */
  public static Builder builder() {
    return new Builder();
  }

  /** Convenience utility for creating {@link DbDocumenterConfig} object. */
  public static final class Builder {
    private List<String> schemas;
    private String databaseHost;
    private String databaseName;
    private int databasePort = 5432;
    private boolean useSsl = true;
    private String username;
    private String password;
    private DatabaseType databaseType = DatabaseType.POSTGRESQL;

    /**
     * Convenience method for supplying a {@link DbDocumenterConfig} object with a list of schema
     * names.
     *
     * @param schemas A {@link List} of schema names. at least 1 schema must be provided.
     * @return {@link DbDocumenterConfig}
     */
    public Builder schemas(final List<String> schemas) {
      this.schemas = List.copyOf(schemas);
      return this;
    }

    /**
     * Convenience method for supplying a {@link DbDocumenterConfig} object with a database host
     * name.
     *
     * @param databaseHost The database host name, must not be blank.
     * @return {@link DbDocumenterConfig}
     */
    public Builder databaseHost(final String databaseHost) {
      this.databaseHost = databaseHost;
      return this;
    }

    /**
     * Convenience method for supplying a {@link DbDocumenterConfig} object with a database port.
     *
     * @param databasePort The database port, defaults to 5432.
     * @return {@link DbDocumenterConfig}
     */
    public Builder databasePort(final int databasePort) {
      this.databasePort = databasePort;
      return this;
    }

    /**
     * Convenience method for supplying a {@link DbDocumenterConfig} object with a database name.
     *
     * @param databaseName The database name, must not be blank.
     * @return {@link DbDocumenterConfig}
     */
    public Builder databaseName(final String databaseName) {
      this.databaseName = databaseName;
      return this;
    }

    /**
     * Convenience method for configuring a {@link DbDocumenterConfig} object to use ssl.
     *
     * @param useSsl If the database connection should use ssl, defaults to TRUE.
     * @return {@link DbDocumenterConfig}
     */
    public Builder useSsl(final boolean useSsl) {
      this.useSsl = useSsl;
      return this;
    }

    /**
     * Convenience method for supplying a {@link DbDocumenterConfig} object with a username.
     *
     * @param username The username of the user used to connect to the database, must not be blank.
     * @return {@link DbDocumenterConfig}
     */
    public Builder username(final String username) {
      this.username = username;
      return this;
    }

    /**
     * Convenience method for supplying a {@link DbDocumenterConfig} object with a password.
     *
     * @param password The password of the user used to connect to the database, must not be blank.
     * @return {@link Builder}
     */
    public Builder password(final String password) {
      this.password = password;
      return this;
    }

    /**
     * Convenience method for supplying a {@link DbDocumenterConfig} object with a {@link
     * DatabaseType}.
     *
     * @param databaseType The type of database to connect to.
     * @return {@link Builder}
     */
    public Builder databaseType(final DatabaseType databaseType) {
      this.databaseType = databaseType;
      return this;
    }

    /**
     * Creates a {@link DbDocumenterConfig} from the parameters provided to the {@link Builder}.
     * Also validates the provided properties
     *
     * @return {@link DbDocumenterConfig}
     */
    public DbDocumenterConfig build() {
      validate();
      return new DbDocumenterConfig(
          schemas,
          databaseHost,
          databasePort,
          databaseName,
          useSsl,
          username,
          password,
          databaseType);
    }

    private void validate() {
      Validators.containsAtLeast1Item(schemas, "schemas");
      Validators.isNotBlank(databaseHost, "databaseHost");
      Validators.isNotBlank(databaseName, "databaseName");
      Validators.isNotBlank(username, "username");
      Validators.isNotBlank(password, "password");
      Validators.isNotNull(databaseType, "databaseType");
    }
  }
}
