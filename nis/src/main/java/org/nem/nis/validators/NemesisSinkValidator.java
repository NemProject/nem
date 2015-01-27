package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;

/**
 * A TransactionValidator implementation that applies to all transactions and validates that:
 * - nemesis account transactions are not allowed after the nemesis block
 */
public class NemesisSinkValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final boolean isNemesisBlock = 0 == context.getBlockHeight().compareTo(BlockHeight.ONE);
		final boolean isNemesisTransaction = transaction.getSigner().getAddress().equals(NemesisBlock.ADDRESS);
		return isNemesisTransaction && !isNemesisBlock
				? ValidationResult.FAILURE_NEMESIS_ACCOUNT_TRANSACTION_AFTER_NEMESIS_BLOCK
				: ValidationResult.SUCCESS;
	}
}
