package db.documenter.internal.formatter.impl.multiplicity;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.ReferentialAction;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MultiplicityFormatter} implementation that appends ON DELETE and ON UPDATE referential
 * action labels to relationship lines in PlantUML format.
 *
 * <p>Only non-{@link ReferentialAction#NO_ACTION} actions are rendered. When both actions are
 * {@code NO_ACTION}, the line is returned unchanged. When one or both are non-default, a PlantUML
 * label is appended using the {@code : "label"} syntax.
 *
 * <p>Labels use {@code " / "} as a separator when both actions are present: {@code "ON DELETE
 * CASCADE / ON UPDATE RESTRICT"}.
 *
 * <p>This formatter must run after {@link CardinalityFormatter} in the pipeline, as it appends to
 * the already-transformed connector line rather than modifying the connector itself.
 *
 * @see CardinalityFormatter
 * @see MultiplicityFormatter
 */
public final class ReferentialActionFormatter implements MultiplicityFormatter {

  /**
   * Appends a referential action label to the relationship line when either action is non-{@link
   * ReferentialAction#NO_ACTION}.
   *
   * @param foreignKey the foreign key whose actions are inspected
   * @param currentSchemaName the schema name of the source table (unused by this formatter)
   * @param current the relationship line produced by earlier formatters in the pipeline
   * @return the line unchanged when both actions are {@code NO_ACTION}; otherwise the line with a
   *     PlantUML {@code : "label"} suffix appended
   */
  @Override
  public String format(
      final ForeignKey foreignKey, final String currentSchemaName, final String current) {
    final List<String> parts = new ArrayList<>();

    if (foreignKey.onDeleteAction() != ReferentialAction.NO_ACTION) {
      parts.add("ON DELETE " + toDisplayName(foreignKey.onDeleteAction()));
    }
    if (foreignKey.onUpdateAction() != ReferentialAction.NO_ACTION) {
      parts.add("ON UPDATE " + toDisplayName(foreignKey.onUpdateAction()));
    }

    if (parts.isEmpty()) {
      return current;
    }

    return current + " : \"" + String.join(" / ", parts) + "\"";
  }

  private String toDisplayName(final ReferentialAction action) {
    return action.name().replace('_', ' ');
  }
}
