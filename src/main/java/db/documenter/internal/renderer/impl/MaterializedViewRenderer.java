package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.renderer.api.PumlRenderer;
import java.util.stream.Collectors;

/**
 * Renders database materialized views as PlantUML entity definitions.
 *
 * <p>Materialized views are rendered with the {@code <<materialized_view>>} stereotype to
 * distinguish them from regular views and tables. Because materialized views in PostgreSQL cannot
 * have primary key or foreign key constraints, all columns are rendered flat in declaration order
 * with no separator line ({@code --}) between groups.
 *
 * <p><b>Column Formatting Decision:</b> This renderer formats columns inline rather than delegating
 * to {@link db.documenter.internal.formatter.api.EntityLineFormatter}. See {@link ViewRenderer} for
 * the full rationale; the same reasoning applies here.
 *
 * <p><b>PlantUML Output Format:</b>
 *
 * <pre>
 * entity "monthly_sales_summary" &lt;&lt;materialized_view&gt;&gt; {
 *   month : date
 *   total_revenue : numeric(19,2)
 *   order_count : integer
 * }
 * </pre>
 *
 * @see MaterializedView
 * @see ViewRenderer
 */
public final class MaterializedViewRenderer implements PumlRenderer<MaterializedView> {

  @Override
  public String render(final MaterializedView materializedView) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append(
        String.format("\tentity \"%s\" <<materialized_view>> {%n", materializedView.name()));

    materializedView
        .columns()
        .forEach(column -> stringBuilder.append(formatColumn(column)).append('\n'));

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
