package dev.shanelucy.dbdocumenter.internal.catalog;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import dev.shanelucy.dbdocumenter.catalog.Column;
import dev.shanelucy.dbdocumenter.catalog.DataType;
import dev.shanelucy.dbdocumenter.catalog.EnumType;
import dev.shanelucy.dbdocumenter.catalog.ForeignKey;
import dev.shanelucy.dbdocumenter.catalog.ReferentialAction;
import dev.shanelucy.dbdocumenter.catalog.Schema;
import dev.shanelucy.dbdocumenter.catalog.Table;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Writes a {@link CatalogSnapshot} as the line-oriented text dump specified in {@code
 * docs/catalog-dump-format.md}.
 *
 * <p>The dump is the golden format for reader integration tests: one self-contained fact per line,
 * so a failing comparison shows a readable diff. Lines follow the model's canonical order and are
 * never re-sorted here, so the dump also pins the model's ordering. The product name and version
 * are left out, so one expected dump can serve several server versions.
 *
 * <p>Any change to the output changes every golden file; keep it in step with the format document.
 *
 * <p><b>Thread Safety:</b> stateless and thread-safe.
 *
 * @see Identifiers
 * @see CatalogDumpOptions
 */
public final class CatalogDumpWriter {

  private static final char LINE_END = '\n';

  private CatalogDumpWriter() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Writes the dump of a snapshot.
   *
   * @return the dump, with {@code \n} line endings and a trailing newline; empty if the snapshot
   *     has no schemas
   */
  public static String write(final CatalogSnapshot snapshot, final CatalogDumpOptions options) {
    Validators.isNotNull(snapshot, "snapshot");
    Validators.isNotNull(options, "options");
    // A switch expression, so adding a mode fails compilation until it is handled here.
    return switch (options.mode()) {
      case VERBATIM -> writeVerbatim(snapshot);
    };
  }

  private static String writeVerbatim(final CatalogSnapshot snapshot) {
    final var lines = new ArrayList<String>();
    for (final Schema schema : snapshot.schemas()) {
      lines.add("schema " + Identifiers.format(schema.name()));
      schema.tables().forEach(table -> appendTable(lines, table));
      schema.enumTypes().forEach(enumType -> lines.add(enumLine(enumType)));
    }
    return lines.stream().map(line -> line + LINE_END).collect(Collectors.joining());
  }

  private static void appendTable(final List<String> lines, final Table table) {
    final String tableName = Identifiers.format(table.name());
    lines.add("table " + tableName);
    table.columns().forEach(column -> lines.add(columnLine(tableName, column)));
    table
        .primaryKey()
        .ifPresent(key -> lines.add(keyLine("pk", tableName, key.columns(), key.name())));
    table
        .uniqueKeys()
        .forEach(key -> lines.add(keyLine("unique", tableName, key.columns(), key.name())));
    table.foreignKeys().forEach(key -> lines.add(foreignKeyLine(tableName, key)));
  }

  private static String columnLine(final String tableName, final Column column) {
    final DataType dataType = column.dataType();
    return "column "
        + tableName
        + '.'
        + Identifiers.format(column.name())
        + " type="
        + Identifiers.quote(dataType.displayText())
        + " nullable="
        + column.nullable()
        + dataType
            .userType()
            .map(userType -> " user_type=" + Identifiers.format(userType))
            .orElse("");
  }

  /** Formats a {@code pk} or {@code unique} line, which share one shape. */
  private static String keyLine(
      final String kind,
      final String tableName,
      final List<String> columns,
      final Optional<String> name) {
    return kind + ' ' + tableName + columnList(columns) + nameSuffix(name);
  }

  private static String foreignKeyLine(final String tableName, final ForeignKey key) {
    return "fk "
        + tableName
        + columnList(key.columns())
        + " -> "
        + Identifiers.format(key.referencedTable())
        + columnList(key.referencedColumns())
        + " on_delete="
        + action(key.onDelete())
        + " on_update="
        + action(key.onUpdate())
        + nameSuffix(key.name());
  }

  private static String enumLine(final EnumType enumType) {
    final String values =
        enumType.values().stream()
            .map(Identifiers::quote)
            .collect(Collectors.joining(", ", "(", ")"));
    return "enum " + Identifiers.format(enumType.name()) + " values=" + values;
  }

  private static String columnList(final List<String> columns) {
    return columns.stream().map(Identifiers::format).collect(Collectors.joining(", ", "(", ")"));
  }

  private static String nameSuffix(final Optional<String> name) {
    return name.map(value -> " name=" + Identifiers.format(value)).orElse("");
  }

  /**
   * Returns the dump token for an action.
   *
   * <p>An explicit mapping rather than {@code name().toLowerCase()}, so renaming an enum constant
   * cannot silently change the golden files.
   */
  private static String action(final ReferentialAction action) {
    return switch (action) {
      case NO_ACTION -> "no_action";
      case RESTRICT -> "restrict";
      case CASCADE -> "cascade";
      case SET_NULL -> "set_null";
      case SET_DEFAULT -> "set_default";
    };
  }
}
