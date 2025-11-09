package db.documenter.internal.models.db;

public record ForeignKey(
    String name, String sourceTable, String sourceColumn, String targetTable, String targetColumn) {
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder sourceTable(final String sourceTable) {
      this.sourceTable = sourceTable;
      return this;
    }

    public Builder sourceColumn(final String sourceColumn) {
      this.sourceColumn = sourceColumn;
      return this;
    }

    public Builder targetTable(final String targetTable) {
      this.targetTable = targetTable;
      return this;
    }

    public Builder targetColumn(final String targetColumn) {
      this.targetColumn = targetColumn;
      return this;
    }

    public ForeignKey build() {
      return new ForeignKey(name, sourceTable, sourceColumn, targetTable, targetColumn);
    }
  }
}
