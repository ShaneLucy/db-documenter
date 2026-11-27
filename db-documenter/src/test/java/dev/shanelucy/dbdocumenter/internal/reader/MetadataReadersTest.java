package dev.shanelucy.dbdocumenter.internal.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.reader.MetadataReaders.Registration;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MetadataReadersTest {

  private Connection connection;
  private DatabaseMetaData metaData;
  private MetadataReader postgresReader;
  private MetadataReader otherReader;

  private static MetadataReader reader(final String productName) {
    return (connection, request) ->
        CatalogSnapshot.builder().productName(productName).productVersion("1").build();
  }

  @BeforeEach
  void setUp() {
    connection = mock(Connection.class);
    metaData = mock(DatabaseMetaData.class);
    postgresReader = reader("PostgreSQL");
    otherReader = reader("Other");
  }

  private void givenProduct(final String name, final String version) throws SQLException {
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn(name);
    when(metaData.getDatabaseProductVersion()).thenReturn(version);
  }

  @Nested
  class ForConnectionTests {

    @Test
    void itThrowsExceptionForPostgresqlOlderThanMinimumVersion() throws SQLException {
      givenProduct("PostgreSQL", "14.12");

      final var exception =
          assertThrows(
              SQLFeatureNotSupportedException.class,
              () -> MetadataReaders.forConnection(connection));

      assertEquals(
          "No metadata reader supports database product 'PostgreSQL' version '14.12'",
          exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"15.0", "15.3", "16.4", "18.1 (Debian 18.1-1.pgdg13+1)", "19devel"})
    void itReturnsPgCatalogReaderForSupportedPostgresql(final String version) throws SQLException {
      givenProduct("PostgreSQL", version);

      assertInstanceOf(PgCatalogReader.class, MetadataReaders.forConnection(connection));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.0.2", "14.99", "", "unknown"})
    void itThrowsExceptionForPostgresqlVersionBelowMinimumOrUnparseable(final String version)
        throws SQLException {
      givenProduct("PostgreSQL", version);

      final var exception =
          assertThrows(
              SQLFeatureNotSupportedException.class,
              () -> MetadataReaders.forConnection(connection));

      assertEquals(
          "No metadata reader supports database product 'PostgreSQL' version '" + version + "'",
          exception.getMessage());
    }

    @Test
    void itThrowsExceptionForOtherProductAtSupportedVersion() throws SQLException {
      givenProduct("MySQL", "18.0");

      final var exception =
          assertThrows(
              SQLFeatureNotSupportedException.class,
              () -> MetadataReaders.forConnection(connection));

      assertEquals(
          "No metadata reader supports database product 'MySQL' version '18.0'",
          exception.getMessage());
    }

    @Test
    void itDoesNotCloseConnection() throws SQLException {
      givenProduct("PostgreSQL", "14.12");

      assertThrows(
          SQLFeatureNotSupportedException.class, () -> MetadataReaders.forConnection(connection));

      verify(connection, never()).close();
    }

    @Test
    void itPropagatesSqlExceptionFromGetMetaData() throws SQLException {
      final var failure = new SQLException("connection is closed");
      when(connection.getMetaData()).thenThrow(failure);

      final var exception =
          assertThrows(SQLException.class, () -> MetadataReaders.forConnection(connection));

      assertSame(failure, exception);
    }
  }

  @Nested
  class FindTests {

    @Test
    void itReturnsReaderForMatchingProductName() throws SQLException {
      givenProduct("PostgreSQL", "16.4");
      final var registry =
          new MetadataReaders(List.of(Registration.anyVersion("PostgreSQL", postgresReader)));

      assertSame(postgresReader, registry.find(connection));
    }

    @Test
    void itReturnsReaderWhenFineLoggingIsEnabled() throws SQLException {
      final var logger = Logger.getLogger(MetadataReaders.class.getName());
      final var originalLevel = logger.getLevel();
      logger.setLevel(Level.FINE);
      try {
        final var registry =
            new MetadataReaders(List.of(Registration.anyVersion("PostgreSQL", postgresReader)));

        assertSame(postgresReader, registry.find("PostgreSQL", "16.4"));
      } finally {
        logger.setLevel(originalLevel);
      }
    }

    @Test
    void itReturnsFirstMatchingRegistration() throws SQLException {
      final var registry =
          new MetadataReaders(
              List.of(
                  Registration.anyVersion("PostgreSQL", postgresReader),
                  Registration.anyVersion("PostgreSQL", otherReader)));

      assertSame(postgresReader, registry.find("PostgreSQL", "16.4"));
    }

    @Test
    void itSkipsRegistrationsForOtherProducts() throws SQLException {
      final var registry =
          new MetadataReaders(
              List.of(
                  Registration.anyVersion("MySQL", otherReader),
                  Registration.anyVersion("PostgreSQL", postgresReader)));

      assertSame(postgresReader, registry.find("PostgreSQL", "16.4"));
    }

    @Test
    void itFallsThroughToNextRegistrationWhenVersionIsRejected() throws SQLException {
      final var registry =
          new MetadataReaders(
              List.of(
                  new Registration("MySQL", version -> version.contains("MariaDB"), otherReader),
                  Registration.anyVersion("MySQL", postgresReader)));

      assertSame(postgresReader, registry.find("MySQL", "8.4.0"));
      assertSame(otherReader, registry.find("MySQL", "5.5.5-11.4.2-MariaDB"));
    }

    @Test
    void itThrowsExceptionWhenVersionPredicateRejectsVersion() {
      final var registry =
          new MetadataReaders(
              List.of(new Registration("PostgreSQL", version -> false, postgresReader)));

      final var exception =
          assertThrows(
              SQLFeatureNotSupportedException.class, () -> registry.find("PostgreSQL", "16.4"));

      assertEquals(
          "No metadata reader supports database product 'PostgreSQL' version '16.4'",
          exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"postgresql", "POSTGRESQL", "PostgreSQL ", "Postgres"})
    void itDoesNotMatchProductNameThatDiffers(final String productName) {
      final var registry =
          new MetadataReaders(List.of(Registration.anyVersion("PostgreSQL", postgresReader)));

      final var exception =
          assertThrows(
              SQLFeatureNotSupportedException.class, () -> registry.find(productName, "16.4"));

      assertEquals(
          "No metadata reader supports database product '" + productName + "' version '16.4'",
          exception.getMessage());
    }
  }

  @Nested
  class FromMajorVersionTests {

    @ParameterizedTest
    @ValueSource(
        strings = {"15", "15.0", "15.3", "16.4", "18.1 (Debian 18.1-1.pgdg13+1)", "19devel"})
    void itAcceptsVersionAtOrAboveMinimumMajorVersion(final String version) throws SQLException {
      final var registry =
          new MetadataReaders(
              List.of(Registration.fromMajorVersion("PostgreSQL", 15, postgresReader)));

      assertSame(postgresReader, registry.find("PostgreSQL", version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"14", "14.12", "9.6.24", "8.0.2", "", "abc", "v15", "99999999999"})
    void itRejectsVersionBelowMinimumOrWithoutLeadingNumber(final String version) {
      final var registry =
          new MetadataReaders(
              List.of(Registration.fromMajorVersion("PostgreSQL", 15, postgresReader)));

      final var exception =
          assertThrows(
              SQLFeatureNotSupportedException.class, () -> registry.find("PostgreSQL", version));

      assertEquals(
          "No metadata reader supports database product 'PostgreSQL' version '" + version + "'",
          exception.getMessage());
    }

    @Test
    void itFallsThroughToOlderReaderWhenVersionIsBelowMinimum() throws SQLException {
      final var registry =
          new MetadataReaders(
              List.of(
                  Registration.fromMajorVersion("PostgreSQL", 15, postgresReader),
                  Registration.anyVersion("PostgreSQL", otherReader)));

      assertSame(otherReader, registry.find("PostgreSQL", "14.12"));
      assertSame(postgresReader, registry.find("PostgreSQL", "15.0"));
    }
  }

  @Nested
  class MajorVersionTests {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "15|15",
          "15.3|15",
          "18.1 (Debian 18.1-1.pgdg13+1)|18",
          "19devel|19",
          "19beta1|19",
          "' 16.4 '|16",
          "007.1|7",
          "0|0",
          "2147483647|2147483647"
        })
    void itParsesLeadingDigits(final String version, final int expected) {
      assertEquals(OptionalInt.of(expected), MetadataReaders.majorVersion(version));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abc", "v15", "-15", ".15", "2147483648", "99999999999999999999"})
    void itReturnsEmptyWhenVersionHasNoLeadingIntMajor(final String version) {
      assertEquals(OptionalInt.empty(), MetadataReaders.majorVersion(version));
    }
  }

  @Nested
  class ConstructorTests {

    @Test
    void itThrowsExceptionWhenRegistrationsContainsNull() {
      final var registrations =
          Arrays.asList(Registration.anyVersion("PostgreSQL", postgresReader), null);

      final var exception =
          assertThrows(ValidationException.class, () -> new MetadataReaders(registrations));

      assertEquals("registrations must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenRegistrationsIsNull() {
      final var exception =
          assertThrows(ValidationException.class, () -> new MetadataReaders(null));

      assertEquals("registrations must not be null", exception.getMessage());
    }
  }

  @Nested
  class RegistrationTests {

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void itThrowsExceptionWhenProductNameIsBlank(final String productName) {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new Registration(productName, version -> true, postgresReader));

      assertEquals("productName must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenVersionPredicateIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new Registration("PostgreSQL", null, postgresReader));

      assertEquals("versionPredicate must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenReaderIsNull() {
      final var exception =
          assertThrows(
              ValidationException.class,
              () -> new Registration("PostgreSQL", version -> true, null));

      assertEquals("reader must not be null", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"16.4", "9.6.24", "", "unknown"})
    void itAcceptsEveryVersionWhenRegisteredForAnyVersion(final String version)
        throws SQLException {
      final var registry =
          new MetadataReaders(List.of(Registration.anyVersion("PostgreSQL", postgresReader)));

      assertSame(postgresReader, registry.find("PostgreSQL", version));
    }
  }
}
