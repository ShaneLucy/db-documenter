/**
 * The metadata reader SPI: reads a {@link dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot} from
 * a JDBC connection.
 *
 * <p>Not exported. {@link dev.shanelucy.dbdocumenter.internal.reader.MetadataReaders} selects a
 * vendor-specific {@link dev.shanelucy.dbdocumenter.internal.reader.MetadataReader} for a
 * connection; vendor implementations live in sub-packages.
 *
 * @see dev.shanelucy.dbdocumenter.internal.reader.MetadataReader
 * @see dev.shanelucy.dbdocumenter.internal.reader.ReadRequest
 */
package dev.shanelucy.dbdocumenter.internal.reader;
