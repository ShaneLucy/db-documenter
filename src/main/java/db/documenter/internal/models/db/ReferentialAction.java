package db.documenter.internal.models.db;

/**
 * Represents the referential integrity action applied to dependent rows when the referenced row is
 * deleted or updated, as defined by the SQL standard.
 *
 * <p>Values correspond to the ANSI {@code DELETE_RULE} and {@code UPDATE_RULE} strings exposed by
 * {@code information_schema.referential_constraints}. The decode logic from these strings to enum
 * values is handled by the result set mapper layer.
 *
 * @see ForeignKey
 */
public enum ReferentialAction {

  /**
   * No action is taken immediately; a deferred constraint check may still raise an error. This is
   * the SQL standard default when no referential action is specified.
   */
  NO_ACTION,

  /** Prevents the delete or update if any dependent rows exist, raising an error immediately. */
  RESTRICT,

  /** Automatically deletes or updates all dependent rows to match the referenced row's change. */
  CASCADE,

  /**
   * Sets the foreign key columns in dependent rows to {@code NULL} when the referenced row changes.
   */
  SET_NULL,

  /**
   * Sets the foreign key columns in dependent rows to their column default when the referenced row
   * changes.
   */
  SET_DEFAULT
}
