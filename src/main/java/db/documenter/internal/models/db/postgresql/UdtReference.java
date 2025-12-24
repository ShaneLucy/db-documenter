package db.documenter.internal.models.db.postgresql;

import db.documenter.internal.validation.Validators;

/**
 * Reference to a PostgreSQL user-defined type (UDT) identified by schema and type name.
 *
 * <p>Represents the location where a UDT (such as an enum or composite type) is defined in the
 * database. This is used to resolve cross-schema type references, ensuring that columns correctly
 * reference their defining types even when the type is defined in a different schema than the table
 * using it.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Cross-Schema Example:</b>
 *
 * <pre>{@code
 * // auth.account_status enum defined in 'auth' schema
 * // core.users table has column 'status' that uses auth.account_status
 *
 * UdtReference udtRef = new UdtReference("auth", "account_status");
 * // Even though the column is in 'core' schema, the UDT is defined in 'auth'
 * }</pre>
 *
 * @param udtSchema the schema where the user-defined type is defined
 * @param udtName the name of the user-defined type
 */
public record UdtReference(String udtSchema, String udtName) {

  public UdtReference {
    Validators.isNotBlank(udtSchema, "udtSchema");
    Validators.isNotBlank(udtName, "udtName");
  }
}
