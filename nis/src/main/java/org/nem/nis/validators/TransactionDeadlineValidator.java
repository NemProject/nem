package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.TimeProvider;

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
		// TODO 20150123 BR -> J,G: I think we should reject a transaction if any child has expired too.
		return transaction.getDeadline().compareTo(this.timeProvider.getCurrentTime()) >= 0
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_PAST_DEADLINE;
	}
}
