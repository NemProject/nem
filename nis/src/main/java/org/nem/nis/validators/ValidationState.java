package org.nem.nis.validators;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Amount;

/**
 * Stateful information associated with a validation.
 */
public class ValidationState {
	private final DebitPredicate<Amount> xemDebitPredicate;
	private final DebitPredicate<Mosaic> mosaicDebitPredicate;

	/**
	 * Creates a validation state with custom debit predicates.
	 *
	 * @param xemDebitPredicate The XEM debit predicate.
	 * @param mosaicDebitPredicate The mosaic debit predicate.
	 */
	public ValidationState(final DebitPredicate<Amount> xemDebitPredicate, final DebitPredicate<Mosaic> mosaicDebitPredicate) {
		this.xemDebitPredicate = xemDebitPredicate;
		this.mosaicDebitPredicate = mosaicDebitPredicate;
	}

	/**
	 * Determines if the specified XEM can be debited from the specified account.
	 *
	 * @param account The account.
	 * @param amount The XEM amount.
	 * @return true if the amount can be debited.
	 */
	public boolean canDebit(final Account account, final Amount amount) {
		return this.xemDebitPredicate.canDebit(account, amount);
	}

	/**
	 * Determines if the specified mosaic can be debited from the specified account.
	 *
	 * @param account The account.
	 * @param mosaic The mosaic.
	 * @return true if the amount can be debited.
	 */
	public boolean canDebit(final Account account, final Mosaic mosaic) {
		return this.mosaicDebitPredicate.canDebit(account, mosaic);
	}
}
