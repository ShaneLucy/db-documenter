package db.documenter.internal.renderer.impl;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;

public final class EntityRenderer implements PumlRenderer<Table> {

  private final EntityLineFormatter lineFormatter;

  public EntityRenderer(final EntityLineFormatter lineFormatter) {
    this.lineFormatter = lineFormatter;
  }

  @Override
  public String render(final Table table) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("\tentity \"%s\" {%n", table.name()));

    final List<String> primaryKeyNames =
        table.primaryKey().map(PrimaryKey::columnNames).orElse(List.of());

    final List<Column> primaryKeyColumns =
        table.columns().stream().filter(column -> primaryKeyNames.contains(column.name())).toList();

    final List<Column> nonPrimaryKeyColumns =
        table.columns().stream()
            .filter(column -> !primaryKeyNames.contains(column.name()))
            .toList();

    primaryKeyColumns.forEach(
        column -> {
          final var formattedLine = lineFormatter.format(table, column, null);
          stringBuilder.append("\t\t").append(formattedLine).append("\n");
        });

    if (!primaryKeyColumns.isEmpty() && !nonPrimaryKeyColumns.isEmpty()) {
      stringBuilder.append("\t\t--\n");
    }

    nonPrimaryKeyColumns.forEach(
        column -> {
          final var formattedLine = lineFormatter.format(table, column, null);
          stringBuilder.append("\t\t").append(formattedLine).append("\n");
        });

    stringBuilder.append("\t}\n");
    return stringBuilder.toString();
  }
}
