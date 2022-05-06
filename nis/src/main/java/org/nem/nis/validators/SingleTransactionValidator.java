package org.nem.nis.validators;

import org.nem.core.model.*;

/**
 * Interface for validating a single transaction.
 */
public interface SingleTransactionValidator extends NamedValidator {

	/**
	 * Checks the validity of the specified transaction.
	 *
	 * @param transaction The transaction.
	 * @param context The validation context.
	 * @return The validation result.
	 */
	ValidationResult validate(final Transaction transaction, final ValidationContext context);
}
