/**
 * Validation utilities for enforcing runtime null-safety and data integrity.
 *
 * <p>This package provides validation methods used by domain models to enforce invariants at
 * construction time. All validators throw {@link
 * dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException} with descriptive error
 * messages when validation fails.
 *
 * @see dev.shanelucy.dbdocumenter.internal.validation.Validators
 */
@NullMarked
package dev.shanelucy.dbdocumenter.internal.validation;

import org.jspecify.annotations.NullMarked;
