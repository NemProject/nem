package org.nem.nis.test;

import org.nem.nis.validators.DebitPredicate;

/**
 * Debit predicates used for testing.
 */
public class DebitPredicates {

	/**
	 * A debit predicate that always returns true.
	 */
	public static final DebitPredicate True = (account, amount) -> true;

	/**
	 * A debit predicate that throws when called.
	 */
	public static final DebitPredicate Throw = (account, amount) -> { throw new UnsupportedOperationException("a DebitPredicate call was unexpected"); };
}
