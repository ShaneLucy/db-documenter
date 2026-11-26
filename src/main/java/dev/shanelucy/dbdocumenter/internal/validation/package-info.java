/**
 * Validation utilities for enforcing runtime null-safety and data integrity.
 *
 * <p>This package provides validation methods used by domain models to enforce invariants at
 * construction time. All validators throw {@link
 * db.documenter.internal.exceptions.ValidationException} with descriptive error messages when
 * validation fails.
 *
 * @see db.documenter.internal.validation.Validators
 */
@NullMarked
package db.documenter.internal.validation;

import org.jspecify.annotations.NullMarked;
