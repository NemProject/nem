package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.cache.*;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that:
 * - the transaction fee is at least as large as the minimum fee
 */
public class MinimumFeeValidator implements SingleTransactionValidator {
	private final ReadOnlyNamespaceCache namespaceCache;
	private final boolean ignoreFees;

	/**
	 * Creates a validator.
	 *
	 * @param namespaceCache The namespace cache.
	 * @param ignoreFees Flag indicating if transaction fees should be ignored.
	 */
	public MinimumFeeValidator(final ReadOnlyNamespaceCache namespaceCache, final boolean ignoreFees) {
		this.namespaceCache = namespaceCache;
		this.ignoreFees = ignoreFees;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (this.ignoreFees) {
			return ValidationResult.SUCCESS;
		}

		final NamespaceCacheLookupAdapters adapters = new NamespaceCacheLookupAdapters(this.namespaceCache);
		final TransactionFeeCalculator calculator = new DefaultTransactionFeeCalculator(adapters.asMosaicFeeInformationLookup());
		return calculator.isFeeValid(transaction, context.getBlockHeight())
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_INSUFFICIENT_FEE;
	}
}
