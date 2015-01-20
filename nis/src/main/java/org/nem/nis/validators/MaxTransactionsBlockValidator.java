package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.BlockChainConstants;

/**
 * A block validator that ensures a block does not have more than the maximum number of transactions.
 */
public class MaxTransactionsBlockValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		final int numTransactions = block.getTransactions().stream()
				.map(t -> 1 + t.getChildTransactions().size())
				.reduce(0, Integer::sum);

		return numTransactions <= BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
	}
}
