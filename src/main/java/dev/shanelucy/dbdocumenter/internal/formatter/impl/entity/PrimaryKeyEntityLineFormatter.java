package dev.shanelucy.dbdocumenter.internal.formatter.impl.entity;

import dev.shanelucy.dbdocumenter.internal.formatter.api.EntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.models.db.Column;
import dev.shanelucy.dbdocumenter.internal.models.db.Table;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class PrimaryKeyEntityLineFormatter implements EntityLineFormatter {

  @Override
  public String format(final Table table, final Column column, final @Nullable String current) {
    final var line = Validators.isNotNull(current, "current");
    final var primaryKeys = table.primaryKey().map(pk -> pk.columnNames()).orElse(List.of());
    if (primaryKeys.contains(column.name())) {
      return "**" + line + "**";
    }
    return line;
  }
}
