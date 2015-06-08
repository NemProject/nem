package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.validators.ValidationContext;

/**
 * A TransferTransactionValidator implementation that applies to transfer transactions.
 */
public class TransferTransactionValidator implements TSingleTransactionValidator<TransferTransaction> {
	private static final int MAX_MESSAGE_SIZE = 160;

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		if (transaction.getMessageLength() > MAX_MESSAGE_SIZE) {
			return ValidationResult.FAILURE_MESSAGE_TOO_LARGE;
		}

		return ValidationResult.SUCCESS;
	}
}
