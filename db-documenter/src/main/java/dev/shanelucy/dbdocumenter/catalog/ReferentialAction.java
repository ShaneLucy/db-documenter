package dev.shanelucy.dbdocumenter.catalog;

/**
 * What a {@link ForeignKey} does to referencing rows when the referenced row is deleted or updated.
 *
 * <p>Constant order is part of the canonical foreign key order, so new constants must be added at
 * the end.
 */
public enum ReferentialAction {
  /** Reject the change if referencing rows remain, checked at the end of the statement. */
  NO_ACTION,
  /** Reject the change immediately if referencing rows exist. */
  RESTRICT,
  /** Delete or update the referencing rows to match. */
  CASCADE,
  /** Set the referencing columns to {@code NULL}. */
  SET_NULL,
  /** Set the referencing columns to their default values. */
  SET_DEFAULT
}
