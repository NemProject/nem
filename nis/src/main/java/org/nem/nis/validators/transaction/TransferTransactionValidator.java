package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.validators.ValidationContext;

/**
 * A TransferTransactionValidator implementation that applies to transfer transactions.
 */
public class TransferTransactionValidator implements TSingleTransactionValidator<TransferTransaction> {
	private static final int ORIGINAL_MAX_MESSAGE_SIZE = 96;
	private static final int MAX_MESSAGE_SIZE_MULTISIG_FORK = 160;
	private static final int CURRENT_MAX_MESSAGE_SIZE = 1024;

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		final int maxMessageLength = context.getBlockHeight().getRaw() >= BlockMarkerConstants.REMOTE_ACCOUNT_FORK(transaction.getVersion())
				? CURRENT_MAX_MESSAGE_SIZE
				: context.getBlockHeight().getRaw() >= BlockMarkerConstants.MULTISIG_M_OF_N_FORK(transaction.getVersion())
						? MAX_MESSAGE_SIZE_MULTISIG_FORK
						: ORIGINAL_MAX_MESSAGE_SIZE;
		if (transaction.getMessageLength() > maxMessageLength) {
			return ValidationResult.FAILURE_MESSAGE_TOO_LARGE;
		}

		return ValidationResult.SUCCESS;
	}
}
