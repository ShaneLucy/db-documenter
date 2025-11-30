package db.documenter.internal.renderer.impl;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RelationshipRenderer implements PumlRenderer<List<Table>> {

  private final MultiplicityFormatter multiplicityFormatter;

  public RelationshipRenderer(final MultiplicityFormatter multiplicityFormatter) {
    this.multiplicityFormatter = multiplicityFormatter;
  }

  @Override
  public String render(final List<Table> tables) {
    final var stringBuilder = new StringBuilder();

    final Map<String, List<ForeignKey>> foreignKeysByTargetTable =
        tables.stream()
            .flatMap(table -> table.foreignKeys().stream())
            .collect(Collectors.groupingBy(ForeignKey::targetTable));

    foreignKeysByTargetTable.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              for (final ForeignKey fk : entry.getValue()) {
                final var formattedLine = multiplicityFormatter.format(fk, null);
                stringBuilder.append(formattedLine).append("\n");
              }
              stringBuilder.append("\n");
            });

    return stringBuilder.toString();
  }
}
