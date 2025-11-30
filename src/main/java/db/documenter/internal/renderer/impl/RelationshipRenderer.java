package db.documenter.internal.renderer.impl;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class RelationshipRenderer implements PumlRenderer<Schema> {
  private static final Logger LOGGER = Logger.getLogger(RelationshipRenderer.class.getName());

  private final MultiplicityFormatter multiplicityFormatter;

  public RelationshipRenderer(final MultiplicityFormatter multiplicityFormatter) {
    this.multiplicityFormatter = multiplicityFormatter;
  }

  @Override
  public String render(final Schema schema) {
    final var stringBuilder = new StringBuilder();

    final Map<String, List<ForeignKey>> foreignKeysByTargetTable =
        schema.tables().stream()
            .flatMap(table -> table.foreignKeys().stream())
            .collect(Collectors.groupingBy(ForeignKey::targetTable));

    final int totalRelationships =
        foreignKeysByTargetTable.values().stream().mapToInt(List::size).sum();

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Rendering {0} relationship(s)", totalRelationships);
    }

    foreignKeysByTargetTable.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              for (final ForeignKey fk : entry.getValue()) {
                final var formattedLine = multiplicityFormatter.format(fk, schema.name(), null);
                stringBuilder.append(formattedLine).append("\n");
              }
              stringBuilder.append("\n");
            });

    return stringBuilder.toString();
  }
}
