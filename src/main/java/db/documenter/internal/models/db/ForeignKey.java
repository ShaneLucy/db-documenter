package db.documenter.internal.models.db;

import db.documenter.internal.validation.Validators;
import org.jspecify.annotations.NonNull;

public record ForeignKey(
    @NonNull String name,
    @NonNull String sourceTable,
    @NonNull String sourceColumn,
    @NonNull String targetTable,
    @NonNull String targetColumn,
    @NonNull String referencedSchema,
    boolean isNullable) {

  public ForeignKey {
    Validators.isNotNull(name, "name");
    Validators.isNotNull(sourceTable, "sourceTable");
    Validators.isNotNull(sourceColumn, "sourceColumn");
    Validators.isNotNull(targetTable, "targetTable");
    Validators.isNotNull(targetColumn, "targetColumn");
    Validators.isNotNull(referencedSchema, "referencedSchema");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
    private String referencedSchema;
    private boolean isNullable;

    public Builder name(final @NonNull String name) {
      this.name = name;
      return this;
    }

    public Builder sourceTable(final @NonNull String sourceTable) {
      this.sourceTable = sourceTable;
      return this;
    }

    public Builder sourceColumn(final @NonNull String sourceColumn) {
      this.sourceColumn = sourceColumn;
      return this;
    }

    public Builder targetTable(final @NonNull String targetTable) {
      this.targetTable = targetTable;
      return this;
    }

    public Builder targetColumn(final @NonNull String targetColumn) {
      this.targetColumn = targetColumn;
      return this;
    }

    public Builder referencedSchema(final @NonNull String referencedSchema) {
      this.referencedSchema = referencedSchema;
      return this;
    }

    public Builder isNullable(final boolean isNullable) {
      this.isNullable = isNullable;
      return this;
    }

    public ForeignKey build() {
      return new ForeignKey(
          name, sourceTable, sourceColumn, targetTable, targetColumn, referencedSchema, isNullable);
    }
  }
}
