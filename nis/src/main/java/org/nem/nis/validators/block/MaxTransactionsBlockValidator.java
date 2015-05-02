package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.validators.BlockValidator;

/**
 * A block validator that ensures a block does not have more than the maximum number of transactions.
 */
public class MaxTransactionsBlockValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		final long numTransactions = BlockExtensions.streamDefault(block).count();
		return numTransactions <= BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
	}
}
