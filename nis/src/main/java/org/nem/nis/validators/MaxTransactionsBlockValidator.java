package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.secret.BlockChainConstants;

/**
 * A block validator that ensures a block does not have more than the maximum number of transactions.
 */
public class MaxTransactionsBlockValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		// TODO 20141102 J-*: there are some blocks with > 60 transactions so we need to add a block marker constant
		if (block.getHeight().getRaw() > BlockMarkerConstants.BETA_HARD_FORK) {
			return block.getTransactions().size() <= BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK
					? ValidationResult.SUCCESS
					: ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
		}
		return ValidationResult.SUCCESS;
	}
}
