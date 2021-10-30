package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that:<br>
 * - the transaction timestamp is before the transaction deadline<br>
 * - the transaction deadline is no more than one day past the transaction timestamp
 */
public class DeadlineValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final TimeInstant timeStamp = transaction.getTimeStamp();
		final TimeInstant deadline = transaction.getDeadline();

		if (timeStamp.compareTo(deadline) >= 0) {
			return ValidationResult.FAILURE_PAST_DEADLINE;
		}

		if (deadline.compareTo(timeStamp.addDays(1)) > 0) {
			return ValidationResult.FAILURE_FUTURE_DEADLINE;
		}

		return ValidationResult.SUCCESS;
	}
}
