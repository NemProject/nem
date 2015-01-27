package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that:
 * - the transaction timestamp is before the transaction deadline
 * - the transaction deadline is no more than one day past the transaction timestamp
 * - the transaction signer has a sufficient balance to cover the transaction fee
 * - the transaction fee is at least as large as the minimum fee
 */
public class UniversalTransactionValidator implements SingleTransactionValidator {

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

		if (!context.getDebitPredicate().canDebit(transaction.getDebtor(), transaction.getFee())) {
			return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
		}

		if (transaction.getMinimumFee().compareTo(transaction.getFee()) > 0) {
			return ValidationResult.FAILURE_INSUFFICIENT_FEE;
		}

		return ValidationResult.SUCCESS;
	}
}
