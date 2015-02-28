package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.validators.ValidationContext;

import java.util.function.Predicate;

/**
 * A TransferTransactionValidator implementation that applies to transfer transactions.
 */
public class TransferTransactionValidator implements TSingleTransactionValidator<TransferTransaction> {
	private static final int MAX_MESSAGE_SIZE = 96;

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		// TODO 20150228 J-J: add tests
		final Predicate<Amount> canDebit = amount -> context.getDebitPredicate().canDebit(transaction.getSigner(), amount);
		final boolean isSelfTransfer = transaction.getSigner().equals(transaction.getRecipient());
		if (isSelfTransfer && context.getBlockHeight().getRaw() < BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK) {
			if (!canDebit.test(transaction.getFee()) || !canDebit.test(transaction.getAmount())) {
				return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
			}
		} else {
			final Amount amount = transaction.getAmount().add(transaction.getFee());
			if (!canDebit.test(amount)) {
				return ValidationResult.FAILURE_INSUFFICIENT_BALANCE;
			}
		}

		if (transaction.getMessageLength() > MAX_MESSAGE_SIZE) {
			return ValidationResult.FAILURE_MESSAGE_TOO_LARGE;
		}

		return ValidationResult.SUCCESS;
	}
}
