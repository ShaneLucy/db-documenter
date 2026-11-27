package dev.shanelucy.dbdocumenter.internal.reader;

import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogReader;
import dev.shanelucy.dbdocumenter.internal.utils.LogUtils;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Selects the {@link MetadataReader} for a connection from its {@link
 * DatabaseMetaData#getDatabaseProductName() product name} and {@link
 * DatabaseMetaData#getDatabaseProductVersion() product version}.
 *
 * <p>Product names are matched exactly as the driver reports them. The version predicate exists for
 * drivers that report one product name for distinct servers (MariaDB through some MySQL drivers) or
 * for readers that need a minimum server version. Registrations are tried in order and the first
 * match wins.
 *
 * <p>The default registry holds {@link PgCatalogReader} for PostgreSQL 15 and later. Older
 * PostgreSQL servers, and products that report the name {@code PostgreSQL} with an older version
 * (such as Amazon Redshift's {@code 8.0.2}), are rejected up front rather than failing on a catalog
 * column they lack.
 *
 * <p><b>Thread Safety:</b> immutable and thread-safe.
 */
public final class MetadataReaders {

  private static final Logger LOGGER = Logger.getLogger(MetadataReaders.class.getName());

  /** The oldest PostgreSQL major version the PostgreSQL reader supports. */
  static final int MINIMUM_POSTGRESQL_MAJOR_VERSION = 15;

  private static final MetadataReaders DEFAULT =
      new MetadataReaders(
          List.of(
              Registration.fromMajorVersion(
                  "PostgreSQL", MINIMUM_POSTGRESQL_MAJOR_VERSION, new PgCatalogReader())));

  private final List<Registration> registrations;

  /**
   * Creates a registry over the given registrations, tried in list order.
   *
   * @param registrations the registrations; defensively copied
   */
  MetadataReaders(final List<Registration> registrations) {
    this.registrations =
        List.copyOf(Validators.containsNoNullElements(registrations, "registrations"));
  }

  /**
   * Returns the reader for the database behind {@code connection}.
   *
   * @param connection an open connection; only its metadata is consulted, and it is not closed
   * @return the first registered reader matching the product name and version
   * @throws SQLFeatureNotSupportedException if no reader supports the product and version
   * @throws SQLException if the product name or version cannot be read
   */
  public static MetadataReader forConnection(final Connection connection) throws SQLException {
    return DEFAULT.find(connection);
  }

  /**
   * Returns the reader for the database behind {@code connection} among this registry's
   * registrations.
   *
   * @throws SQLFeatureNotSupportedException if no reader supports the product and version
   * @throws SQLException if the product name or version cannot be read
   */
  MetadataReader find(final Connection connection) throws SQLException {
    final DatabaseMetaData metaData = connection.getMetaData();
    return find(metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
  }

  /**
   * Returns the first reader registered for the product name whose version predicate accepts the
   * version.
   *
   * @throws SQLFeatureNotSupportedException if no registration matches
   */
  MetadataReader find(final String productName, final String productVersion)
      throws SQLFeatureNotSupportedException {
    final MetadataReader reader =
        registrations.stream()
            .filter(registration -> registration.matches(productName, productVersion))
            .map(Registration::reader)
            .findFirst()
            .orElseThrow(
                () ->
                    new SQLFeatureNotSupportedException(
                        "No metadata reader supports database product '"
                            + productName
                            + "' version '"
                            + productVersion
                            + "'"));
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(
          Level.FINE,
          "Selected {0} for {1} {2}",
          new Object[] {
            reader.getClass().getName(),
            LogUtils.sanitizeForLog(productName),
            LogUtils.sanitizeForLog(productVersion)
          });
    }
    return reader;
  }

  /**
   * One registry entry: a reader for a product name, limited to versions its predicate accepts.
   *
   * <p><b>Immutability:</b> immutable if the predicate and reader are, which readers must be.
   *
   * @param productName the product name exactly as the driver reports it; not blank
   * @param versionPredicate accepts the product versions the reader supports
   * @param reader the reader to use
   */
  record Registration(
      String productName, Predicate<String> versionPredicate, MetadataReader reader) {

    /** Validates the components. */
    Registration {
      Validators.isNotBlank(productName, "productName");
      Validators.isNotNull(versionPredicate, "versionPredicate");
      Validators.isNotNull(reader, "reader");
    }

    /** Creates a registration that accepts every version of the product. */
    static Registration anyVersion(final String productName, final MetadataReader reader) {
      return new Registration(productName, version -> true, reader);
    }

    /**
     * Creates a registration that accepts versions whose {@linkplain #majorVersion major version}
     * is at least {@code minimumMajorVersion}; a version without a leading number is rejected.
     */
    static Registration fromMajorVersion(
        final String productName, final int minimumMajorVersion, final MetadataReader reader) {
      return new Registration(
          productName,
          version -> {
            final OptionalInt major = majorVersion(version);
            return major.isPresent() && major.getAsInt() >= minimumMajorVersion;
          },
          reader);
    }

    private boolean matches(final String name, final String version) {
      return productName.equals(name) && versionPredicate.test(version);
    }
  }

  /**
   * Parses the leading major version number of a product version string, such as {@code 15} from
   * {@code 15.3}, {@code 18} from {@code 18.1 (Debian 18.1-1.pgdg13+1)} or {@code 19} from {@code
   * 19devel}.
   *
   * @param productVersion the version as the driver reports it; may be null for a misbehaving
   *     driver
   * @return the digits before the first non-digit, or empty if the string does not start with a
   *     digit or the number does not fit in an {@code int}
   */
  static OptionalInt majorVersion(final @Nullable String productVersion) {
    if (productVersion == null) {
      return OptionalInt.empty();
    }
    final String trimmed = productVersion.strip();
    int end = 0;
    while (end < trimmed.length() && isAsciiDigit(trimmed.charAt(end))) {
      end++;
    }
    if (end == 0) {
      return OptionalInt.empty();
    }
    try {
      return OptionalInt.of(Integer.parseInt(trimmed, 0, end, 10));
    } catch (final NumberFormatException overflow) {
      return OptionalInt.empty();
    }
  }

  private static boolean isAsciiDigit(final char character) {
    return character >= '0' && character <= '9';
  }
}
