package db.documenter.internal.models.db;

import db.documenter.internal.validation.Validators;
import org.jspecify.annotations.NonNull;

/**
 * Composite key uniquely identifying a column within a table.
 *
 * <p>Used as a lookup key when mapping columns to associated database objects such as user-defined
 * types, constraints, or other metadata. This key enables efficient O(1) lookup operations when
 * mapping columns to their associated database properties.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Identify a specific column
 * ColumnKey key = new ColumnKey("users", "account_status");
 *
 * // Use as map key for O(1) lookup
 * Map<ColumnKey, UdtReference> mappings = queryRunner.getColumnUdtMappings("auth");
 * UdtReference udtRef = mappings.get(key);
 * }</pre>
 *
 * @param tableName the table containing this column
 * @param columnName the column name
 * @see Column
 */
public record ColumnKey(@NonNull String tableName, @NonNull String columnName) {

  public ColumnKey {
    Validators.isNotBlank(tableName, "tableName");
    Validators.isNotBlank(columnName, "columnName");
  }
}
