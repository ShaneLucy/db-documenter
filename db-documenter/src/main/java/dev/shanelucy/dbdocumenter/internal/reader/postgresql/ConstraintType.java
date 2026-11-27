package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

/**
 * The {@code pg_constraint.contype} values the constraints query reads.
 *
 * <p>Parsed once when a row is mapped, so an unexpected code fails at the JDBC boundary rather than
 * inside the assembler.
 */
enum ConstraintType {
  PRIMARY_KEY('p'),
  UNIQUE('u'),
  FOREIGN_KEY('f');

  private final char code;

  ConstraintType(final char code) {
    this.code = code;
  }

  /** Returns the {@code contype} code. */
  char code() {
    return code;
  }

  /**
   * Returns the type for a {@code contype} code.
   *
   * @throws IllegalArgumentException if the code is not {@code p}, {@code u} or {@code f}, which
   *     the constraints query never returns
   */
  static ConstraintType fromCode(final char code) {
    for (final ConstraintType type : values()) {
      if (type.code == code) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown PostgreSQL constraint type '" + code + "'");
  }
}
