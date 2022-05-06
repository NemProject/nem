package org.nem.nis.cache;

import org.nem.core.model.TransactionExecutionState;
import org.nem.core.model.mosaic.*;
import org.nem.nis.sync.*;
import org.nem.nis.validators.ValidationState;

/**
 * Static class containing helper functions for [ReadOnly]NisCache.
 */
public class NisCacheUtils {

	/**
	 * Creates a default validation state around a nis cache.
	 *
	 * @param nisCache The nis cache.
	 * @return The validation state.
	 */
	public static ValidationState createValidationState(final ReadOnlyNisCache nisCache) {
		return new ValidationState(new DefaultXemDebitPredicate(nisCache.getAccountStateCache()),
				new DefaultMosaicDebitPredicate(nisCache.getNamespaceCache()), createTransactionExecutionState(nisCache));
	}

	/**
	 * Creates a default transaction execution state around a nis cache.
	 *
	 * @param nisCache The nis cache.
	 * @return The transaction execution state.
	 */
	public static TransactionExecutionState createTransactionExecutionState(final ReadOnlyNisCache nisCache) {
		final NamespaceCacheLookupAdapters adapters = new NamespaceCacheLookupAdapters(nisCache.getNamespaceCache());
		return new TransactionExecutionState(new DefaultMosaicTransferFeeCalculator(adapters.asMosaicLevyLookup()));
	}
}
