package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.validators.*;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that nemesis account transactions are allowed in the
 * nemesis and treasury reissuance blocks.
 */
public class NemesisSinkValidator implements SingleTransactionValidator {
	private final BlockHeight treasuryReissuanceForkHeight;

	/**
	 * Creates a new validator.
	 *
	 * @param treasuryReissuanceForkHeight The treasury reissuance fork height.
	 */
	public NemesisSinkValidator(final BlockHeight treasuryReissuanceForkHeight) {
		this.treasuryReissuanceForkHeight = treasuryReissuanceForkHeight;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final Address nemesisAddress = NetworkInfos.getDefault().getNemesisBlockInfo().getAddress();
		final boolean isNemesisBlock = context.getBlockHeight().equals(BlockHeight.ONE);
		final boolean isTreasuryReissuanceBlock = context.getBlockHeight().equals(treasuryReissuanceForkHeight);
		final boolean isNemesisTransaction = transaction.getSigner().getAddress().equals(nemesisAddress);
		return isNemesisTransaction && !isNemesisBlock && !isTreasuryReissuanceBlock
				? ValidationResult.FAILURE_NEMESIS_ACCOUNT_TRANSACTION_AFTER_NEMESIS_BLOCK
				: ValidationResult.SUCCESS;
	}
}
