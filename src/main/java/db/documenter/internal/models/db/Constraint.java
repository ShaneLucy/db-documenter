package db.documenter.internal.models.db;

public enum Constraint {
  FK(0),
  UNIQUE(1),
  AUTO_INCREMENT(2),
  DEFAULT(3),
  CHECK(4),
  NULLABLE(5);

  private final int displayPriority;

  Constraint(final int displayPriority) {
    this.displayPriority = displayPriority;
  }

  public int displayPriority() {
    return displayPriority;
  }
}
