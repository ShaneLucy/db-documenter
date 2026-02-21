package db.documenter.internal.builder;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.formatter.impl.entity.CompositeEntityLineFormatter;
import db.documenter.internal.formatter.impl.entity.ConstraintEntityLineFormatter;
import db.documenter.internal.formatter.impl.entity.DefaultEntityLineFormatter;
import db.documenter.internal.formatter.impl.entity.PrimaryKeyEntityLineFormatter;
import db.documenter.internal.formatter.impl.multiplicity.CardinalityFormatter;
import db.documenter.internal.formatter.impl.multiplicity.CompositeMultiplicityFormatter;
import db.documenter.internal.formatter.impl.multiplicity.DefaultMultiplicityFormatter;
import db.documenter.internal.formatter.impl.multiplicity.ReferentialActionFormatter;

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
        .addFormatter(new ReferentialActionFormatter())
        .build();
  }
}
