package org.nem.core.model.transactions.extensions;

import org.nem.core.model.Transaction;

/**
 * A transaction validation extension that can be used to run different validation rules for different
 * transaction versions.
 *
 * @param <TTransaction> The type of transaction.
 */
public interface TransactionValidationExtension<TTransaction extends Transaction> {

	/**
	 * Gets a value indicating whether or not this validation applies to the specified transaction version.
	 *
	 * @param version The transaction entity version.
	 * @return true if this validation should be applied.
	 */
	boolean isApplicable(final int version);

	/**
	 * Validates the specified transaction against this rule.
	 *
	 * @param transaction The transaction.
	 */
	void validate(final TTransaction transaction);
}
