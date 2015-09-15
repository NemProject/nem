package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.validators.BlockValidator;

/**
 * A block validator that ensures a block does not have more than the maximum number of transactions.
 */
public class MaxTransactionsBlockValidator implements BlockValidator {
	private final int maxAllowedTransactionsPerBlock;

	public MaxTransactionsBlockValidator(final int maxAllowedTransactionsPerBlock) {
		this.maxAllowedTransactionsPerBlock = maxAllowedTransactionsPerBlock;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final long numTransactions = BlockExtensions.streamDefault(block).count();
		return numTransactions <= this.maxAllowedTransactionsPerBlock
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
	}
}
