package dev.shanelucy.dbdocumenter.internal.formatter.impl.entity;

import dev.shanelucy.dbdocumenter.internal.formatter.api.EntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.models.db.Column;
import dev.shanelucy.dbdocumenter.internal.models.db.Table;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CompositeEntityLineFormatter(List<EntityLineFormatter> lineFormatters)
    implements EntityLineFormatter {

  public CompositeEntityLineFormatter {
    lineFormatters = List.copyOf(Validators.isNotNull(lineFormatters, "lineFormatters"));
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String format(final Table table, final Column column, final @Nullable String current) {
    @Nullable String result = current;
    for (EntityLineFormatter lineFormatter : lineFormatters) {
      result = lineFormatter.format(table, column, result);
    }
    return Validators.isNotNull(result, "formattedLine");
  }

  public static final class Builder {
    private final List<EntityLineFormatter> lineFormatters = new ArrayList<>();

    public Builder addFormatter(final EntityLineFormatter lineFormatter) {
      lineFormatters.add(lineFormatter);
      return this;
    }

    public CompositeEntityLineFormatter build() {
      return new CompositeEntityLineFormatter(lineFormatters);
    }
  }
}
