package db.documenter.internal.models.db;

import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record DbEnum(
    @NonNull String enumName, @NonNull String columnName, @NonNull List<String> enumValues) {

  public DbEnum {
    Validators.isNotNull(enumName, "enumName");
    Validators.isNotNull(columnName, "columnName");
    Validators.isNotNull(enumValues, "enumValues");
    enumValues = List.copyOf(enumValues);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String enumName;
    private String columnName;
    private List<String> enumValues;

    public Builder enumName(final @NonNull String enumName) {
      this.enumName = enumName;
      return this;
    }

    public Builder columnName(final @NonNull String columnName) {
      this.columnName = columnName;
      return this;
    }

    public Builder enumValues(final @NonNull List<String> enumValues) {
      this.enumValues = List.copyOf(enumValues);
      return this;
    }

    public DbEnum build() {
      return new DbEnum(enumName, columnName, enumValues);
    }
  }
}
