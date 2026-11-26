package dev.shanelucy.dbdocumenter.internal.formatter.impl.entity;

import dev.shanelucy.dbdocumenter.internal.formatter.api.EntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.models.db.Column;
import dev.shanelucy.dbdocumenter.internal.models.db.Constraint;
import dev.shanelucy.dbdocumenter.internal.models.db.Table;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class ConstraintEntityLineFormatter implements EntityLineFormatter {

  @Override
  public String format(final Table table, final Column column, final @Nullable String current) {
    final var line = Validators.isNotNull(current, "current");
    if (column.constraints().isEmpty()) {
      return line;
    }

    final var constraintString =
        column.constraints().stream()
            .sorted((c1, c2) -> Integer.compare(c1.displayPriority(), c2.displayPriority()))
            .map(Constraint::name)
            .collect(Collectors.joining(","));

    return line + " <<" + constraintString + ">>";
  }
}
