package db.documenter.internal.models.db;

import db.documenter.internal.validation.Validators;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public record Table(
    @NonNull String name,
    @NonNull List<Column> columns,
    @NonNull Optional<PrimaryKey> primaryKey,
    @NonNull List<ForeignKey> foreignKeys) {

  public Table {
    Validators.isNotNull(name, "name");
    Validators.isNotNull(columns, "columns");
    Validators.isNotNull(primaryKey, "primaryKey");
    Validators.isNotNull(foreignKeys, "foreignKeys");
    columns = List.copyOf(columns);
    foreignKeys = List.copyOf(foreignKeys);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private List<Column> columns;
    private PrimaryKey primaryKey;
    private List<ForeignKey> foreignKeys;

    public Builder name(final @NonNull String name) {
      this.name = name;
      return this;
    }

    public Builder columns(final @NonNull List<Column> columns) {
      this.columns = List.copyOf(columns);
      return this;
    }

    public Builder primaryKey(final PrimaryKey primaryKey) {
      this.primaryKey = primaryKey;
      return this;
    }

    public Builder foreignKeys(final @NonNull List<ForeignKey> foreignKeys) {
      this.foreignKeys = List.copyOf(foreignKeys);
      return this;
    }

    public Table build() {
      return new Table(name, columns, Optional.ofNullable(primaryKey), foreignKeys);
    }
  }
}
