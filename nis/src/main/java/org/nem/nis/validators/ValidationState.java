package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Amount;

/**
 * Stateful information associated with a validation.
 */
public class ValidationState {
	private final DebitPredicate<Amount> xemDebitPredicate;
	private final DebitPredicate<Mosaic> mosaicDebitPredicate;
	private final TransactionExecutionState transactionExecutionState;

	/**
	 * Creates a validation state with custom debit predicates and transaction execution state.
	 *
	 * @param xemDebitPredicate The XEM debit predicate.
	 * @param mosaicDebitPredicate The mosaic debit predicate.
	 * @param transactionExecutionState The transaction execution state.
	 */
	public ValidationState(final DebitPredicate<Amount> xemDebitPredicate, final DebitPredicate<Mosaic> mosaicDebitPredicate,
			final TransactionExecutionState transactionExecutionState) {
		this.xemDebitPredicate = xemDebitPredicate;
		this.mosaicDebitPredicate = mosaicDebitPredicate;
		this.transactionExecutionState = transactionExecutionState;
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

	/**
	 * Gets the transaction execution state.
	 *
	 * @return The transaction execution state.
	 */
	public TransactionExecutionState transactionExecutionState() {
		return this.transactionExecutionState;
	}
}
