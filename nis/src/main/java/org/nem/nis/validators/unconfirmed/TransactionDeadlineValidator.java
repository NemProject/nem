package org.nem.nis.validators.unconfirmed;

import org.nem.core.model.*;
import org.nem.core.time.*;
import org.nem.nis.validators.*;

/**
 * A transaction validator that ensures the transaction has a valid deadline.
 */
public class TransactionDeadlineValidator implements SingleTransactionValidator {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new validator.
	 *
	 * @param timeProvider The time provider.
	 */
	public TransactionDeadlineValidator(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final TimeInstant currentTime = this.timeProvider.getCurrentTime();

		final boolean isExpired = TransactionExtensions.streamDefault(transaction).anyMatch(t -> isExpired(t, currentTime));
		return isExpired ? ValidationResult.FAILURE_PAST_DEADLINE : ValidationResult.SUCCESS;
	}

	private static boolean isExpired(final Transaction transaction, final TimeInstant currentTime) {
		return transaction.getDeadline().compareTo(currentTime) < 0;
	}
}
