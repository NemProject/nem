package org.nem.nis.validators;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;

/**
 * Predicate that can be used to determine if an amount can be debited from an account.
 */
@FunctionalInterface
public interface DebitPredicate {

	/**
	 * Determines if the specified amount can be debited from the specified account.
	 *
	 * @param account The account.
	 * @param amount The amount.
	 * @return true if the amount can be debited.
	 */
	boolean canDebit(final Account account, final Amount amount);
}
