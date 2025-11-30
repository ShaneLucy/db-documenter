package db.documenter.internal.models.db;

import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record Schema(
    @NonNull String name, @NonNull List<Table> tables, @NonNull List<DbEnum> dbEnums) {

  public static Builder builder() {
    return new Builder();
  }

  public Schema {
    Validators.isNotNull(name, "name");
    Validators.isNotNull(tables, "tables");
    Validators.isNotNull(dbEnums, "dbEnums");
    tables = List.copyOf(tables);
    dbEnums = List.copyOf(dbEnums);
  }

  public static class Builder {
    private String name;
    private List<Table> tables;
    private List<DbEnum> dbEnums;

    public Builder name(final @NonNull String name) {
      this.name = name;
      return this;
    }

    public Builder tables(final @NonNull List<Table> tables) {
      this.tables = List.copyOf(tables);
      return this;
    }

    public Builder dbEnums(final @NonNull List<DbEnum> dbEnums) {
      this.dbEnums = List.copyOf(dbEnums);
      return this;
    }

    public Schema build() {
      return new Schema(name, tables, dbEnums);
    }
  }
}
