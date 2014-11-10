package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.*;

/**
 * A block validator that ensures a block does not have more than the maximum number of transactions.
 */
// TODO 2141110 BR -> all: remember to adjust this before launch!!!
public class MaxTransactionsBlockValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		if (block.getHeight().getRaw() <= BlockMarkerConstants.BETA_HARD_FORK) {
			return ValidationResult.SUCCESS;
		}

		if (block.getHeight().getRaw() > BlockMarkerConstants.BETA_TX_COUNT_FORK) {
			return block.getTransactions().size() <= BlockChainConstants.NEW_MAX_ALLOWED_TRANSACTIONS_PER_BLOCK
			       ? ValidationResult.SUCCESS
			       : ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
		}
		return block.getTransactions().size() <= BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
	}
}
