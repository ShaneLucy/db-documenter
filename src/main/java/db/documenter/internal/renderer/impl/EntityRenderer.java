package db.documenter.internal.renderer.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.renderer.api.PumlRenderer;

public class EntityRenderer implements PumlRenderer<Table> {

  private final LineFormatter lineFormatter;

  public EntityRenderer(LineFormatter lineFormatter) {
    this.lineFormatter = lineFormatter;
  }

  @Override
  public String render(Table table) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("\tentity \"%s\" as %s {%n", table.name(), table.name()));

    table
        .columns()
        .forEach(
            column -> {
              final var formattedLine = lineFormatter.format(table, column, null);
              sb.append("\t\t").append(formattedLine).append("\n");
            });

    sb.append("\t}\n");
    return sb.toString();
  }
}
