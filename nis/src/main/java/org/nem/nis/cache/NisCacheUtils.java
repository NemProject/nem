package org.nem.nis.cache;

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
		return new ValidationState(
				new DefaultXemDebitPredicate(nisCache.getAccountStateCache()),
				new DefaultMosaicDebitPredicate(nisCache.getNamespaceCache()));
	}
}
