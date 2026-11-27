package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import dev.shanelucy.dbdocumenter.internal.reader.MetadataReader;
import dev.shanelucy.dbdocumenter.internal.reader.ReadRequest;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads a {@link CatalogSnapshot} from PostgreSQL 15+ by querying {@code pg_catalog} directly.
 *
 * <p><b>Bulk queries:</b> a run issues exactly six statements ({@link PgCatalogQueries}: schemas,
 * tables, columns, constraints, unique indexes, enums), each filtered by {@code nspname = ANY(?)}
 * over all requested schemas, so the statement count does not grow with the number of schemas or
 * tables. The rows are joined by OID in Java by {@link PgCatalogAssembler}.
 *
 * <p><b>Visibility:</b> a schema is read when it exists and the role has {@code USAGE} on it; a
 * requested schema that is missing or lacks {@code USAGE} is absent from the snapshot and logged at
 * {@link Level#WARNING}. A table is read when, in addition, the role has {@code SELECT} on the
 * table or on any of its columns ({@code has_any_column_privilege}, which is also true for a
 * table-level grant); all its columns are then shown. Foreign keys into tables that are out of
 * scope or invisible keep their target: its schema, table and column names come from {@code
 * pg_class} and {@code pg_attribute}, which every role can read.
 *
 * <p><b>Scope:</b> ordinary and partitioned tables are read; partitions (including partitions that
 * are themselves partitioned), views, materialized views and sequences are not. Legacy {@code
 * INHERITS} children are ordinary tables. Constraints cloned onto partitions ({@code conparentid <>
 * 0}) are skipped.
 *
 * <p><b>Nullability:</b> a column is not nullable when it is declared {@code NOT NULL} or its type
 * is a domain that is {@code NOT NULL} anywhere along its domain chain, as pgjdbc reports it. An
 * array of such a domain is nullable.
 *
 * <p><b>Session state:</b> the reader never changes the connection's autocommit mode, isolation,
 * {@code search_path} or any other setting, and its output depends on none of them: catalog objects
 * and operators are qualified with {@code pg_catalog}, user type names come from the raw catalog
 * columns, and {@code format_type} output is normalised by {@link FormatTypeNormaliser} against
 * {@code search_path} shadowing and {@code quote_all_identifiers}.
 *
 * <p><b>Thread Safety:</b> stateless and thread-safe.
 *
 * @see PgCatalogAssembler
 * @see PgCatalogMappings
 */
public final class PgCatalogReader implements MetadataReader {

  private static final Logger LOGGER = Logger.getLogger(PgCatalogReader.class.getName());

  /** Creates a reader; it holds no state, so one instance can be shared. */
  public PgCatalogReader() {
    // Stateless.
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Transactions:</b> the six statements run in the caller's transaction mode. With
   * autocommit on, each sees its own catalog snapshot. Running them inside one {@code REPEATABLE
   * READ} transaction narrows the window but does not close it, because the privilege functions and
   * {@code format_type} read the system caches rather than the transaction snapshot; rows that no
   * longer fit together are dropped and logged either way. With autocommit off, the reader leaves
   * the caller's transaction open, and if a statement fails, that transaction is left aborted; the
   * caller must roll it back.
   */
  @Override
  public CatalogSnapshot read(final Connection connection, final ReadRequest request)
      throws SQLException {
    Validators.isNotNull(connection, "connection");
    Validators.isNotNull(request, "request");
    final DatabaseMetaData metaData = connection.getMetaData();
    final String productName = metaData.getDatabaseProductName();
    final String productVersion = metaData.getDatabaseProductVersion();
    final Array schemaNames =
        connection.createArrayOf("text", request.schemas().toArray(String[]::new));
    try {
      final PgCatalogRows rows =
          PgCatalogQueries.readRows(connection, schemaNames, request.schemas());
      logRowCounts(rows);
      return PgCatalogAssembler.assemble(productName, productVersion, rows);
    } finally {
      schemaNames.free();
    }
  }

  private static void logRowCounts(final PgCatalogRows rows) {
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(
          Level.FINE,
          "Read {0} schemas, {1} tables, {2} columns, {3} constraints, {4} unique indexes and"
              + " {5} enums",
          new Object[] {
            rows.schemaNames().size(),
            rows.tables().size(),
            rows.columns().size(),
            rows.constraints().size(),
            rows.uniqueIndexes().size(),
            rows.enums().size()
          });
    }
  }
}
