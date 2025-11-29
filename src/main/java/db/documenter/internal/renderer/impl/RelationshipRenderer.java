package db.documenter.internal.renderer.impl;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;

public final class RelationshipRenderer implements PumlRenderer<List<Table>> {

  private final MultiplicityFormatter multiplicityFormatter;

  public RelationshipRenderer(final MultiplicityFormatter multiplicityFormatter) {
    this.multiplicityFormatter = multiplicityFormatter;
  }

  @Override
  public String render(final List<Table> tables) {
    final var stringBuilder = new StringBuilder();

    for (final Table table : tables) {
      for (final ForeignKey fk : table.foreignKeys()) {
        final var formattedLine = multiplicityFormatter.format(fk, null);
        stringBuilder.append(formattedLine).append("\n");
      }
    }
    return stringBuilder.toString();
  }
}
