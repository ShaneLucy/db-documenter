package db.documenter.internal.models.db.postgresql;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.validation.Validators;

/**
 * Composite key for uniquely identifying database enums by schema and name.
 *
 * <p>Different schemas may contain enum types with identical names but different allowed values.
 * This record ensures proper identification and grouping of enums by both schema and name,
 * preventing confusion when the same enum name exists in multiple schemas.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Two different enums with the same name in different schemas:
 * // core.project_status: ['ACTIVE', 'COMPLETED', 'CANCELLED']
 * // audit.project_status: ['PENDING', 'APPROVED', 'REJECTED']
 *
 * EnumKey coreStatus = new EnumKey("core", "project_status");
 * EnumKey auditStatus = new EnumKey("audit", "project_status");
 *
 * // These are different enums despite having the same name
 * assert !coreStatus.equals(auditStatus);
 * }</pre>
 *
 * @param schema the schema containing the enum type (e.g., "core", "public")
 * @param name the enum type name (e.g., "project_status")
 * @see DbEnum
 */
public record EnumKey(String schema, String name) {

  public EnumKey {
    Validators.isNotBlank(schema, "schema");
    Validators.isNotBlank(name, "name");
  }
}
