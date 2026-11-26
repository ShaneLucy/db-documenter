package db.documenter.internal.mapper;

import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.validation.Validators;
import java.util.Map;

/**
 * Context for resolving column user-defined types across schemas.
 *
 * <p>Encapsulates all the data needed for cross-schema UDT resolution, including column-to-UDT
 * mappings and enum definitions from all documented schemas.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Create context with mappings and enum definitions
 * ColumnMappingContext context = new ColumnMappingContext(
 *     columnUdtMappings,  // Map<ColumnKey, UdtReference>
 *     enumsByKey,         // Map<EnumKey, DbEnum>
 *     "users",            // current table
 *     "core"              // current schema
 * );
 *
 * // Use in ColumnMapper
 * List<Column> resolved = columnMapper.mapUserDefinedTypes(rawColumns, context);
 * }</pre>
 *
 * @param columnUdtMappings maps (table, column) to their UDT definitions
 * @param enumsByKey maps (schema, enum name) to enum definitions for O(1) lookup
 * @param currentTableName the table being processed
 * @param currentSchema the schema being processed
 */
public record ColumnMappingContext(
    Map<ColumnKey, UdtReference> columnUdtMappings,
    Map<EnumKey, DbEnum> enumsByKey,
    String currentTableName,
    String currentSchema) {

  public ColumnMappingContext {
    Validators.isNotNull(columnUdtMappings, "columnUdtMappings");
    Validators.isNotNull(enumsByKey, "enumsByKey");
    Validators.isNotBlank(currentTableName, "currentTableName");
    Validators.isNotBlank(currentSchema, "currentSchema");

    // Defensive copy for immutability
    columnUdtMappings = Map.copyOf(columnUdtMappings);
    enumsByKey = Map.copyOf(enumsByKey);
  }
}
