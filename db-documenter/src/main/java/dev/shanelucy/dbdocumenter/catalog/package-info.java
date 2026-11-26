/**
 * A vendor-neutral, read-only snapshot of database metadata: schemas, tables, columns, keys and
 * enum types.
 *
 * <p><b>Immutability:</b> every type is an immutable record (or enum) whose lists are defensive
 * copies, so a snapshot is thread-safe and can be shared freely.
 *
 * <p><b>Deterministic ordering:</b> each record puts its lists into one canonical order when it is
 * constructed, so two snapshots of the same catalog are equal and print identically whatever order
 * the metadata was read in. Schemas, tables and enum types are sorted by name; unique keys and
 * foreign keys by their columns. Columns keep table order, key columns keep key order and enum
 * values keep declared order.
 *
 * <p><b>Names:</b> identifiers are exactly as stored in the database catalog, with no case folding,
 * quoting or trimming. They are compared in Java {@link String} order.
 *
 * <p><b>Validation:</b> records validate in their constructors and throw {@link
 * IllegalArgumentException} for invalid input.
 */
package dev.shanelucy.dbdocumenter.catalog;
