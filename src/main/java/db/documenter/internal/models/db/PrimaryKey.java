package db.documenter.internal.models.db;

import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record PrimaryKey(@NonNull String constraintName, @NonNull List<String> columnNames) {

  public PrimaryKey {
    Validators.isNotNull(constraintName, "constraintName");
    Validators.isNotNull(columnNames, "columnNames");
    columnNames = List.copyOf(columnNames);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String constraintName;
    private List<String> columnNames;

    public Builder constraintName(final @NonNull String constraintName) {
      this.constraintName = constraintName;
      return this;
    }

    public Builder columnNames(final @NonNull List<String> columnNames) {
      this.columnNames = List.copyOf(columnNames);
      return this;
    }

    public PrimaryKey build() {
      return new PrimaryKey(constraintName, columnNames);
    }
  }
}
