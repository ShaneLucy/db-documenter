package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.View;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.stream.Collectors;

/**
 * Renders database views as PlantUML entity definitions.
 *
 * <p>Views are rendered with the {@code <<view>>} stereotype. Because views in PostgreSQL cannot
 * have primary key or foreign key constraints, all columns are rendered flat in declaration order
 * with no separator line ({@code --}) between groups.
 *
 * <p><b>Column Formatting Decision:</b> This renderer formats columns inline rather than delegating
 * to {@link db.documenter.internal.formatter.api.EntityLineFormatter}. The existing formatter
 * interface is tightly coupled to {@link db.documenter.internal.models.db.Table} (its first
 * parameter) and exists specifically to handle primary-key bold formatting and
 * primary-key/non-primary-key separation — concepts that do not apply to views. Rendering inline
 * keeps the view renderer simple and avoids constructing stub {@code Table} objects or creating
 * unnecessary adapter types.
 *
 * <p><b>PlantUML Output Format:</b>
 *
 * <pre>
 * entity "active_users_view" &lt;&lt;view&gt;&gt; {
 *   id : uuid
 *   name : varchar(100)
 *   email : varchar(255) &lt;&lt;NULLABLE&gt;&gt;
 * }
 * </pre>
 *
 * @see View
 * @see MaterializedViewRenderer
 */
public final class ViewRenderer implements PumlRenderer<View> {

  @Override
  public String render(final View view) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("\tentity \"%s\" <<view>> {%n", view.name()));

    view.columns().forEach(column -> stringBuilder.append(formatColumn(column)).append('\n'));

    stringBuilder.append("\t}\n");
    return stringBuilder.toString();
  }

  /**
   * Formats a single column as a PlantUML entity line.
   *
   * <p>Produces {@code name: type} or {@code name: type(length)} for character types, followed by
   * constraint annotations in {@code <<...>>} notation when constraints are present.
   *
   * @param column the column to format
   * @return the formatted line with leading tab indentation
   */
  private String formatColumn(final Column column) {
    final String baseType =
        (column.maximumLength() > 0)
            ? String.format("%s(%d)", column.dataType(), column.maximumLength())
            : column.dataType();

    final String base = String.format("\t\t%s: %s", column.name(), baseType);

    if (column.constraints().isEmpty()) {
      return base;
    }

    final String constraintAnnotation =
        column.constraints().stream()
            .sorted((c1, c2) -> Integer.compare(c1.displayPriority(), c2.displayPriority()))
            .map(Constraint::name)
            .collect(Collectors.joining(","));

    return base + " <<" + constraintAnnotation + ">>";
  }
}
