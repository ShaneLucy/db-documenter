package dev.shanelucy.dbdocumenter.internal.exceptions;

/**
 * Signals that a value failed validation.
 *
 * <p>Extends {@link IllegalArgumentException} so that callers of the public API, which cannot
 * reference this internal type from the module path, can still catch it by a public type.
 */
public final class ValidationException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an exception describing the failed validation.
   *
   * @param message what was invalid, including the name of the offending property
   */
  public ValidationException(final String message) {
    super(message);
  }
}
