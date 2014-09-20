package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

/**
 * A TransactionValidator implementation that applies to all transactions.
 */
public class UniversalTransactionValidator implements TransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final DebitPredicate predicate) {
		final TimeInstant timeStamp = transaction.getTimeStamp();
		final TimeInstant deadline = transaction.getDeadline();

		if (timeStamp.compareTo(deadline) >= 0) {
			return ValidationResult.FAILURE_PAST_DEADLINE;
		}

		if (deadline.compareTo(timeStamp.addDays(1)) > 0) {
			return ValidationResult.FAILURE_FUTURE_DEADLINE;
		}

		if (!predicate.canDebit(transaction.getSigner(), transaction.getFee())) {
			return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
		}

		return ValidationResult.SUCCESS;
	}
}
