package db.documenter.internal.renderer.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.renderer.api.PumlRenderer;

public class EntityRenderer implements PumlRenderer<Table> {

  private final LineFormatter lineFormatter;

  public EntityRenderer(final LineFormatter lineFormatter) {
    this.lineFormatter = lineFormatter;
  }

  @Override
  public String render(final Table table) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("\tentity \"%s\" as %s {%n", table.name(), table.name()));

    table
        .columns()
        .forEach(
            column -> {
              final var formattedLine = lineFormatter.format(table, column, null);
              stringBuilder.append("\t\t").append(formattedLine).append("\n");
            });

    stringBuilder.append("\t}\n");
    return stringBuilder.toString();
  }
}
