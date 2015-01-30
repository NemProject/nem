package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;

/**
 * A TransferTransactionValidator implementation that applies to transfer transactions.
 */
public class TransferTransactionValidator implements SingleTransactionValidator {
	private static final int MAX_MESSAGE_SIZE = 96;

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.TRANSFER != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return this.validate((TransferTransaction)transaction, context.getDebitPredicate());
	}

	private ValidationResult validate(final TransferTransaction transaction, final DebitPredicate predicate) {
		final Amount amount = transaction.getAmount().add(transaction.getFee());
		if (!predicate.canDebit(transaction.getSigner(), amount)) {
			return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
		}

		if (transaction.getMessageLength() > MAX_MESSAGE_SIZE) {
			return ValidationResult.FAILURE_MESSAGE_TOO_LARGE;
		}

		return ValidationResult.SUCCESS;
	}
}
