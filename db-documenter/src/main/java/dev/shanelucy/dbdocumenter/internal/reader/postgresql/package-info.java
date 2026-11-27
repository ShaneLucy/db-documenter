/**
 * The PostgreSQL {@link dev.shanelucy.dbdocumenter.internal.reader.MetadataReader}, which reads
 * {@code pg_catalog} directly in a fixed number of bulk queries.
 *
 * <p>Not exported. {@link dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogReader}
 * runs the statements in {@code PgCatalogQueries} and maps result sets to plain rows ({@code
 * PgCatalogRows}) with {@code PgCatalogRowMappers}. {@code PgCatalogAssembler}, {@code
 * PgTableAssembler}, {@code PgCatalogMappings} and {@code FormatTypeNormaliser} turn those rows
 * into the catalog model without touching JDBC, so they can be unit-tested without a database.
 *
 * @see dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogReader
 */
package dev.shanelucy.dbdocumenter.internal.reader.postgresql;
