package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

/**
 * Interface for validating batches of transactions.
 */
public interface BatchTransactionValidator {

	/**
	 * Checks the validity of the specified transactions.
	 *
	 * @param groupedTransactions The grouped transactions.
	 * @return The validation result.
	 */
	public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions);
}