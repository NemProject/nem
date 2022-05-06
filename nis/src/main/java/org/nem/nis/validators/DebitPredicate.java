package org.nem.nis.validators;

import org.nem.core.model.Account;

/**
 * Predicate that can be used to determine if an asset can be debited from an account.
 */
@FunctionalInterface
public interface DebitPredicate<T> {

	/**
	 * Determines if the specified asset can be debited from the specified account.
	 *
	 * @param account The account.
	 * @param asset The asset.
	 * @return true if the amount can be debited.
	 */
	boolean canDebit(final Account account, final T asset);
}
