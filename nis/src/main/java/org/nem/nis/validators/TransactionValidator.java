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
		return this.validate(transaction, (account, amount) -> account.getBalance().compareTo(amount) >= 0);
	}

	/**
	 * Checks the validity of the specified transaction.
	 *
	 * @param transaction The transaction.
	 * @param predicate A predicate for checking account debits.
	 * @return The validation result.
	 */
	public ValidationResult validate(final Transaction transaction, final DebitPredicate predicate);
}
