package dev.shanelucy.dbdocumenter;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Connection and scope settings for a {@link DbDocumenter} run.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe; the schema list is defensively
 * copied.
 *
 * <p><b>Validation:</b> Performed in the compact constructor. Any violation throws an {@link
 * IllegalArgumentException} (a {@code ValidationException} internally) naming the offending
 * property. Defaults for port, SSL and database type are applied by {@link Builder} only; the
 * canonical constructor requires every value explicitly.
 *
 * <p><b>Secrets:</b> {@link #toString()} masks the password, but {@link #equals(Object)} and {@link
 * #hashCode()} are the record defaults and therefore include it.
 *
 * @param schemas names of the schemas to document; at least 1, no null elements
 * @param databaseHost database host name; not blank
 * @param databasePort database port; between 1 and 65535
 * @param databaseName database name; not blank
 * @param useSsl whether the connection uses SSL
 * @param username user used to connect; not blank
 * @param password password of that user; not blank
 * @param databaseType the kind of database being documented; not null
 */
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
   * Validates every component and defensively copies the schema list.
   *
   * @throws IllegalArgumentException if any component is missing, blank or out of range
   */
  public DbDocumenterConfig {
    Validators.containsAtLeast1Item(schemas, "schemas");
    Validators.containsNoNullElements(schemas, "schemas");
    Validators.isNotBlank(databaseHost, "databaseHost");
    Validators.isValidPort(databasePort, "databasePort");
    Validators.isNotBlank(databaseName, "databaseName");
    Validators.isNotBlank(username, "username");
    Validators.isNotBlank(password, "password");
    Validators.isNotNull(databaseType, "databaseType");
    schemas = List.copyOf(schemas);
  }

  /**
   * Returns a representation that masks the password so the configuration is safe to log.
   *
   * @return a string describing this configuration with {@code password=****}
   */
  @Override
  public String toString() {
    return ("DbDocumenterConfig[schemas=%s, databaseHost=%s, databasePort=%d, databaseName=%s,"
            + " useSsl=%s, username=%s, password=****, databaseType=%s]")
        .formatted(
            schemas, databaseHost, databasePort, databaseName, useSsl, username, databaseType);
  }

  /**
   * Creates a new builder for {@link DbDocumenterConfig}.
   *
   * @return a builder with defaults of port 5432, SSL enabled and PostgreSQL
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link DbDocumenterConfig} instances using a fluent API.
   *
   * <p>Defaults: port 5432, SSL enabled, {@link DatabaseType#POSTGRESQL}. Host, database name,
   * username, password and schemas are required.
   */
  public static final class Builder {
    private @Nullable List<String> schemas;
    private @Nullable String databaseHost;
    private @Nullable String databaseName;
    private int databasePort = 5432;
    private boolean useSsl = true;
    private @Nullable String username;
    private @Nullable String password;
    private DatabaseType databaseType = DatabaseType.POSTGRESQL;

    /** Returns this builder for method chaining. */
    public Builder schemas(final List<String> schemas) {
      this.schemas = schemas;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder databaseHost(final String databaseHost) {
      this.databaseHost = databaseHost;
      return this;
    }

    /** Returns this builder for method chaining; the port defaults to 5432. */
    public Builder databasePort(final int databasePort) {
      this.databasePort = databasePort;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder databaseName(final String databaseName) {
      this.databaseName = databaseName;
      return this;
    }

    /** Returns this builder for method chaining; SSL defaults to enabled. */
    public Builder useSsl(final boolean useSsl) {
      this.useSsl = useSsl;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder username(final String username) {
      this.username = username;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder password(final String password) {
      this.password = password;
      return this;
    }

    /** Returns this builder for method chaining; the type defaults to PostgreSQL. */
    public Builder databaseType(final DatabaseType databaseType) {
      this.databaseType = databaseType;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return a new validated {@link DbDocumenterConfig}
     * @throws IllegalArgumentException if a required value is missing or any value is invalid
     */
    public DbDocumenterConfig build() {
      return new DbDocumenterConfig(
          Validators.containsAtLeast1Item(schemas, "schemas"),
          Validators.isNotBlank(databaseHost, "databaseHost"),
          databasePort,
          Validators.isNotBlank(databaseName, "databaseName"),
          useSsl,
          Validators.isNotBlank(username, "username"),
          Validators.isNotBlank(password, "password"),
          Validators.isNotNull(databaseType, "databaseType"));
    }
  }
}
