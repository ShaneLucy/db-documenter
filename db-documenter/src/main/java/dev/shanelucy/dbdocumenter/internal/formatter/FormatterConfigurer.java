package dev.shanelucy.dbdocumenter.internal.formatter;

import dev.shanelucy.dbdocumenter.internal.formatter.api.EntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.api.MultiplicityFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.impl.entity.CompositeEntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.impl.entity.ConstraintEntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.impl.entity.DefaultEntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.impl.entity.PrimaryKeyEntityLineFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.impl.multiplicity.CardinalityFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.impl.multiplicity.CompositeMultiplicityFormatter;
import dev.shanelucy.dbdocumenter.internal.formatter.impl.multiplicity.DefaultMultiplicityFormatter;

/** Configures and creates formatter instances for PlantUML generation. */
public final class FormatterConfigurer {

  /**
   * Creates the composite entity line formatter with all formatting rules.
   *
   * @return configured {@link EntityLineFormatter} instance
   */
  public EntityLineFormatter createEntityLineFormatter() {
    return CompositeEntityLineFormatter.builder()
        .addFormatter(new DefaultEntityLineFormatter())
        .addFormatter(new PrimaryKeyEntityLineFormatter())
        .addFormatter(new ConstraintEntityLineFormatter())
        .build();
  }

  /**
   * Creates the composite multiplicity formatter with all formatting rules.
   *
   * @return configured {@link MultiplicityFormatter} instance
   */
  public MultiplicityFormatter createMultiplicityFormatter() {
    return CompositeMultiplicityFormatter.builder()
        .addFormatter(new DefaultMultiplicityFormatter())
        .addFormatter(new CardinalityFormatter())
        .build();
  }
}
