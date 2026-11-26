package dev.shanelucy.dbdocumenter.internal.formatter.api;

import dev.shanelucy.dbdocumenter.internal.models.db.Column;
import dev.shanelucy.dbdocumenter.internal.models.db.Table;
import org.jspecify.annotations.Nullable;

/**
 * Contract for one link in a chain that builds a single entity line of the ERD.
 *
 * <p>The {@code current} argument is the line accumulated by earlier links in the chain; it is
 * {@code null} for the first link, which is responsible for creating the initial line.
 */
@FunctionalInterface
public interface EntityLineFormatter {

  String format(final Table table, final Column column, final @Nullable String current);
}
