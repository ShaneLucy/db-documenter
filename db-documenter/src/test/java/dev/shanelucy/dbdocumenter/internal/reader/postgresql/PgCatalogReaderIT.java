package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import dev.shanelucy.dbdocumenter.catalog.Column;
import dev.shanelucy.dbdocumenter.catalog.Schema;
import dev.shanelucy.dbdocumenter.internal.catalog.CatalogDumpOptions;
import dev.shanelucy.dbdocumenter.internal.catalog.CatalogDumpWriter;
import dev.shanelucy.dbdocumenter.internal.reader.MetadataReaders;
import dev.shanelucy.dbdocumenter.internal.reader.ReadRequest;
import dev.shanelucy.dbdocumenter.testhelpers.PostgresTestEnvironment;
import dev.shanelucy.dbdocumenter.testhelpers.QueryCountingConnection;
import dev.shanelucy.dbdocumenter.testhelpers.TestResources;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Reads the Phase 1.4 edge-case fixture with {@link PgCatalogReader} and compares the verbatim dump
 * with the hand-written expected dump, on the oldest and newest supported PostgreSQL majors.
 *
 * <p>Each image's {@code @Nested} class has a per-class lifecycle, so it starts one container and
 * loads the fixture once for all its tests. The fixture creates no roles; the least-privilege and
 * scale cases are Phase 1.6.
 */
class PgCatalogReaderIT {

  private static final String FIXTURE = "/postgresql/catalog-edge-cases/catalog-edge-cases.sql";
  private static final String EXPECTED_DUMP =
      "/postgresql/catalog-edge-cases/catalog-edge-cases-expected-catalog.txt";
  private static final List<String> REQUESTED_SCHEMAS =
      List.of("Odd Names", "core", "shop_east", "shop_west");

  /**
   * The statements one read issues (schemas, tables, columns, constraints, unique indexes, enums).
   * The plan caps a run at 8; this pins the current count so any extra query is noticed.
   */
  private static final int EXPECTED_STATEMENT_COUNT = 6;

  /** The tests every image runs against its own shared, fixture-loaded container. */
  @TestInstance(Lifecycle.PER_CLASS)
  abstract class ImageTests {

    private final PostgresTestEnvironment environment;
    private Connection connection;
    private String expectedDump;

    ImageTests(final String image) {
      this.environment = new PostgresTestEnvironment(image);
    }

    @BeforeAll
    void containerSetUp() throws SQLException, IOException {
      environment.startContainer(null);
      connection = environment.getConnection();
      environment.initialiseDatabase(connection, FIXTURE);
      expectedDump = TestResources.readString(EXPECTED_DUMP);
    }

    @AfterAll
    void containerClearDown() throws SQLException {
      environment.stop();
    }

    private CatalogSnapshot read(final Connection readConnection, final List<String> schemas)
        throws SQLException {
      return MetadataReaders.forConnection(readConnection)
          .read(readConnection, new ReadRequest(schemas));
    }

    private static String dump(final CatalogSnapshot snapshot) {
      return CatalogDumpWriter.write(snapshot, CatalogDumpOptions.verbatim());
    }

    private String searchPath(final Connection target) throws SQLException {
      try (var statement = target.createStatement();
          var resultSet = statement.executeQuery("SHOW search_path")) {
        assertTrue(resultSet.next());
        return resultSet.getString(1);
      }
    }

    @Test
    void itSelectsPgCatalogReader() throws SQLException {
      assertInstanceOf(PgCatalogReader.class, MetadataReaders.forConnection(connection));
    }

    @Test
    void itMatchesExpectedDumpByteForByte() throws SQLException {
      final var actual = dump(read(connection, REQUESTED_SCHEMAS));

      // Both sides are decoded as UTF-8, so string equality is byte-for-byte equality.
      assertEquals(expectedDump, actual);
    }

    @Test
    void itIssuesFixedNumberOfStatements() throws SQLException {
      final var counter = QueryCountingConnection.wrap(connection);

      read(counter.connection(), REQUESTED_SCHEMAS);

      assertEquals(EXPECTED_STATEMENT_COUNT, counter.executionCount());
    }

    @Test
    void itIssuesSameNumberOfStatementsForOneSchema() throws SQLException {
      final var counter = QueryCountingConnection.wrap(connection);

      read(counter.connection(), List.of("core"));

      assertEquals(EXPECTED_STATEMENT_COUNT, counter.executionCount());
    }

    @Test
    void itLeavesDefaultSessionStateUnchangedAndConnectionOpen() throws SQLException {
      final var autoCommit = connection.getAutoCommit();
      final var isolation = connection.getTransactionIsolation();
      final var searchPath = searchPath(connection);

      read(connection, REQUESTED_SCHEMAS);

      assertFalse(connection.isClosed());
      assertEquals(autoCommit, connection.getAutoCommit());
      assertEquals(isolation, connection.getTransactionIsolation());
      assertEquals(searchPath, searchPath(connection));
    }

    /**
     * A session with non-default autocommit, isolation and a {@code search_path} that puts the
     * fixture's schemas (and their same-named types) first: the reader must leave all three alone,
     * and the dump must not depend on {@code search_path} (plan decision B1).
     */
    @Test
    void itLeavesCustomSessionStateUnchangedAndDumpIndependentOfSearchPath() throws SQLException {
      try (Connection session = environment.getContainer().createConnection("")) {
        session.setAutoCommit(false);
        session.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        try (var statement = session.createStatement()) {
          statement.execute("SET search_path = core, shop_east, \"Odd Names\", public");
        }
        final var searchPath = searchPath(session);

        final var actual = dump(read(session, REQUESTED_SCHEMAS));

        assertEquals(expectedDump, actual);
        assertFalse(session.isClosed());
        assertFalse(session.getAutoCommit());
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, session.getTransactionIsolation());
        assertEquals(searchPath, searchPath(session));
        session.rollback();
      }
    }

    @Test
    void itOmitsRequestedSchemaThatDoesNotExist() throws SQLException {
      final var schemas =
          List.of("Odd Names", "core", "no_such_schema", "shop_east", "shop_west", "CORE");

      final var snapshot = read(connection, schemas);

      assertEquals(
          REQUESTED_SCHEMAS.stream().sorted().toList(),
          snapshot.schemas().stream().map(Schema::name).toList());
      assertEquals(expectedDump, dump(snapshot));
    }

    @Test
    void itReturnsNoSchemasWhenNoneExist() throws SQLException {
      final var snapshot = read(connection, List.of("no_such_schema"));

      assertEquals(List.of(), snapshot.schemas());
    }

    /** Runs each statement on its own, in order, on the given connection. */
    private static void execute(final Connection target, final String... statements)
        throws SQLException {
      try (var statement = target.createStatement()) {
        for (final String sql : statements) {
          statement.execute(sql);
        }
      }
    }

    /**
     * A session whose {@code search_path} puts a schema of same-named domains and always-false
     * {@code =} operators before {@code pg_catalog}, with {@code quote_all_identifiers} on. Every
     * operator and type the queries use is qualified and {@code format_type} output is normalised,
     * so the dump and the statement count must not change.
     */
    @Test
    void itIgnoresShadowingTypesAndOperatorsAndQuoteAllIdentifiers() throws SQLException {
      execute(
          connection,
          "DROP SCHEMA IF EXISTS shadow CASCADE",
          "CREATE SCHEMA shadow",
          "CREATE DOMAIN shadow.text AS integer",
          "CREATE DOMAIN shadow.int4 AS integer",
          "CREATE DOMAIN shadow.uuid AS integer",
          "CREATE FUNCTION shadow.never_equal(oid, oid) RETURNS boolean"
              + " LANGUAGE sql IMMUTABLE AS 'SELECT false'",
          "CREATE FUNCTION shadow.never_equal(name, text) RETURNS boolean"
              + " LANGUAGE sql IMMUTABLE AS 'SELECT false'",
          "CREATE OPERATOR shadow.= (LEFTARG = oid, RIGHTARG = oid,"
              + " FUNCTION = shadow.never_equal)",
          "CREATE OPERATOR shadow.= (LEFTARG = name, RIGHTARG = text,"
              + " FUNCTION = shadow.never_equal)");
      try (Connection session = environment.getContainer().createConnection("")) {
        execute(session, "SET search_path = shadow, pg_catalog", "SET quote_all_identifiers = on");
        assertProbeSeesShadowOperator(session);
        final var counter = QueryCountingConnection.wrap(session);

        final var actual = dump(read(counter.connection(), REQUESTED_SCHEMAS));

        assertEquals(expectedDump, actual);
        assertEquals(EXPECTED_STATEMENT_COUNT, counter.executionCount());
      } finally {
        execute(connection, "DROP SCHEMA shadow CASCADE");
      }
    }

    /** Guards the hostile case itself: an unqualified {@code =} must hit the shadow operator. */
    private void assertProbeSeesShadowOperator(final Connection session) throws SQLException {
      try (var statement = session.createStatement();
          var resultSet =
              statement.executeQuery(
                  "SELECT count(*) FROM pg_catalog.pg_namespace WHERE nspname = 'shadow'::pg_catalog.text")) {
        assertTrue(resultSet.next());
        assertEquals(0, resultSet.getInt(1));
      }
    }

    /**
     * A domain named {@code text} in the session's temporary schema shadows {@code pg_catalog.text}
     * even under the default {@code search_path}, because {@code pg_temp} is searched first for
     * types; {@code format_type} then qualifies the built-in type.
     */
    @Test
    void itIgnoresTemporaryDomainShadowingBuiltInType() throws SQLException {
      try (Connection session = environment.getContainer().createConnection("")) {
        execute(session, "CREATE DOMAIN pg_temp.text AS integer");

        final var actual = dump(read(session, REQUESTED_SCHEMAS));

        assertEquals(expectedDump, actual);
      }
    }

    /**
     * A NOT NULL domain anywhere along a column type's domain chain makes the column not nullable,
     * but an array of it stays nullable (plan finding S6). The schema is created and dropped here
     * so the shared fixture and its expected dump stay untouched.
     */
    @Test
    void itTreatsColumnOfNotNullDomainChainAsNotNullable() throws SQLException {
      execute(
          connection,
          "DROP SCHEMA IF EXISTS s6_domains CASCADE",
          "CREATE SCHEMA s6_domains",
          "CREATE DOMAIN s6_domains.d1 AS integer NOT NULL",
          "CREATE DOMAIN s6_domains.d2 AS s6_domains.d1",
          "CREATE DOMAIN s6_domains.plain AS integer",
          "CREATE TABLE s6_domains.t (a s6_domains.d2, b s6_domains.d1, c s6_domains.plain,"
              + " d s6_domains.d2[])");
      try {
        final var snapshot = read(connection, List.of("s6_domains"));

        final var nullability =
            snapshot.schemas().getFirst().tables().getFirst().columns().stream()
                .map(Column::nullable)
                .toList();
        assertEquals(List.of(false, false, true, true), nullability);
        assertEquals(
            """
            schema s6_domains
            table s6_domains.t
            column s6_domains.t.a type="d2" nullable=false user_type=s6_domains.d2
            column s6_domains.t.b type="d1" nullable=false user_type=s6_domains.d1
            column s6_domains.t.c type="plain" nullable=true user_type=s6_domains.plain
            column s6_domains.t.d type="d2[]" nullable=true user_type=s6_domains.d2
            """,
            dump(snapshot));
      } finally {
        execute(connection, "DROP SCHEMA s6_domains CASCADE");
      }
    }

    @Test
    void itLogsWarningForRequestedSchemaThatDoesNotExist() throws SQLException {
      final var logCapture = LogCapture.attach(PgCatalogAssembler.class.getName());
      try {
        read(connection, List.of("core", "no_such_schema"));

        assertEquals(
            List.of(
                "Requested schema no_such_schema is not in the snapshot because it does not"
                    + " exist"),
            logCapture.warnings());
      } finally {
        logCapture.detach();
      }
    }

    @Test
    void itProducesSameDumpWhenReadTwice() throws SQLException {
      final var first = dump(read(connection, REQUESTED_SCHEMAS));
      final var second = dump(read(connection, REQUESTED_SCHEMAS));

      assertEquals(first, second);
    }
  }

  @Nested
  class OldestSupportedImageTests extends ImageTests {
    OldestSupportedImageTests() {
      super(PostgresTestEnvironment.DEFAULT_IMAGE);
    }
  }

  @Nested
  class LatestSupportedImageTests extends ImageTests {
    LatestSupportedImageTests() {
      super(PostgresTestEnvironment.LATEST_SUPPORTED_IMAGE);
    }
  }
}
