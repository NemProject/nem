package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that:
 * - the transaction timestamp is before the transaction deadline
 * - the transaction deadline is no more than one day past the transaction timestamp
 * - the transaction signer has a sufficient balance to cover the transaction fee
 * - the transaction fee is at least as large as the minimum fee
 */
public class UniversalTransactionValidator implements SingleTransactionValidator {
	private final ReadOnlyNamespaceCache namespaceCache;

	/**
	 * Creates a validator.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public UniversalTransactionValidator(final ReadOnlyNamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final TimeInstant timeStamp = transaction.getTimeStamp();
		final TimeInstant deadline = transaction.getDeadline();

		if (timeStamp.compareTo(deadline) >= 0) {
			return ValidationResult.FAILURE_PAST_DEADLINE;
		}

		if (deadline.compareTo(timeStamp.addDays(1)) > 0) {
			return ValidationResult.FAILURE_FUTURE_DEADLINE;
		}

		final NamespaceCacheLookupAdapters adapters = new NamespaceCacheLookupAdapters(this.namespaceCache);
		final TransactionFeeCalculator calculator = new DefaultTransactionFeeCalculator(adapters.asMosaicFeeInformationLookup());
		if (!calculator.isFeeValid(transaction, context.getBlockHeight())) {
			return ValidationResult.FAILURE_INSUFFICIENT_FEE;
		}

		return ValidationResult.SUCCESS;
	}
}
