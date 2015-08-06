package org.nem.nis.test;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.validators.DebitPredicate;

/**
 * Debit predicates used for testing.
 */
public class DebitPredicates {

	/**
	 * A XEM debit predicate that throws when called.
	 */
	public static final DebitPredicate<Amount> XemThrow = (account, amount) -> {
		throw new UnsupportedOperationException("a DebitPredicate<Amount> call was unexpected");
	};

	/**
	 * A mosaic debit predicate that throws when called.
	 */
	public static final DebitPredicate<Mosaic> MosaicThrow = (account, mosaic) -> {
		throw new UnsupportedOperationException("a DebitPredicate<Mosaic> call was unexpected");
	};
}
