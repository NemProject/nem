package org.nem.nis.validators;

import org.nem.core.model.ValidationResult;

import java.util.List;

/**
 * Interface for validating batches of transactions.
 */
public interface BatchTransactionValidator extends NamedValidator {

	/**
	 * Checks the validity of the specified transactions.
	 *
	 * @param groupedTransactions The grouped transactions.
	 * @return The validation result.
	 */
	ValidationResult validate(final List<TransactionsContextPair> groupedTransactions);
}
