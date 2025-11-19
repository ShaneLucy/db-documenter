package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;

public class RelationshipRenderer implements PumlRenderer<List<Table>> {

  @Override
  public String render(final List<Table> tables) {
    final var stringBuilder = new StringBuilder();

    for (final Table table : tables) {
      for (final ForeignKey fk : table.foreignKeys()) {
        stringBuilder.append(
            String.format(
                "%s::%s --> %s::%s : %s%n",
                fk.sourceTable(),
                fk.sourceColumn(),
                fk.targetTable(),
                fk.targetColumn(),
                fk.name()));
      }
    }
    return stringBuilder.toString();
  }
}
