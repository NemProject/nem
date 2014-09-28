package org.nem.nis.validators;

import org.nem.core.model.*;

/**
 * Interface for validating a transaction.
 */
public interface TransactionValidator {

	/**
	 * Checks the validity of the specified transaction.
	 *
	 * @param transaction The transaction.
	 * @return The validation result.
	 */
	default public ValidationResult validate(final Transaction transaction) {
		return this.validate(transaction, new ValidationContext());
	}

	/**
	 * Checks the validity of the specified transaction.
	 *
	 * @param transaction The transaction.
	 * @param context The validation context.
	 * @return The validation result.
	 */
	public ValidationResult validate(final Transaction transaction, final ValidationContext context);
}
