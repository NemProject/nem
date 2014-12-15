package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.TimeProvider;

/**
 * A transaction validator that ensures the transaction has a valid deadline.
 */
public class TransactionDeadlineValidator implements SingleTransactionValidator {
	private final TimeProvider timeProvider;

	public TransactionDeadlineValidator(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return transaction.getDeadline().compareTo(this.timeProvider.getCurrentTime()) >= 0 ? ValidationResult.SUCCESS : ValidationResult.FAILURE_PAST_DEADLINE;
	}
}
