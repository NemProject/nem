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
	public ValidationResult validate(final Transaction transaction);
}
