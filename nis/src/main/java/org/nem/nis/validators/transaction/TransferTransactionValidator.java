package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.validators.ValidationContext;

/**
 * A TransferTransactionValidator implementation that applies to transfer transactions.
 */
public class TransferTransactionValidator implements TSingleTransactionValidator<TransferTransaction> {
	private static final int ORIGINAL_MAX_MESSAGE_SIZE = 96;
	private static final int MAX_MESSAGE_SIZE_MULTISIG_FORK = 160;
	private static final int CURRENT_MAX_MESSAGE_SIZE = 1024;
	private final BlockHeight remoteAccountForkHeight;
	private final BlockHeight multisigMOfNForkHeight;

	/**
	 * Creates a transfer transaction validator.
	 *
	 * @param remoteAccountForkHeight The remote account fork height.
	 * @param multisigMOfNForkHeight The multisig M-of-N fork height.
	 */
	public TransferTransactionValidator(final BlockHeight remoteAccountForkHeight, final BlockHeight multisigMOfNForkHeight) {
		this.remoteAccountForkHeight = remoteAccountForkHeight;
		this.multisigMOfNForkHeight = multisigMOfNForkHeight;
	}

	@Override
	public ValidationResult validate(final TransferTransaction transaction, final ValidationContext context) {
		final int maxMessageLength = context.getBlockHeight().getRaw() >= this.remoteAccountForkHeight.getRaw()
				? CURRENT_MAX_MESSAGE_SIZE
				: context.getBlockHeight().getRaw() >= this.multisigMOfNForkHeight.getRaw()
						? MAX_MESSAGE_SIZE_MULTISIG_FORK
						: ORIGINAL_MAX_MESSAGE_SIZE;
		if (transaction.getMessageLength() > maxMessageLength) {
			return ValidationResult.FAILURE_MESSAGE_TOO_LARGE;
		}

		return ValidationResult.SUCCESS;
	}
}
