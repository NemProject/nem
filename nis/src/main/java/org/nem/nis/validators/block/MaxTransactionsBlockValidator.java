package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.validators.BlockValidator;

/**
 * A block validator that ensures a block does not have more than the maximum number of transactions.
 */
public class MaxTransactionsBlockValidator implements BlockValidator {
	private final int maxTransactionsPerBlock;

	/**
	 * Creates a validator.
	 *
	 * @param maxTransactionsPerBlock The maximum number of transactions per block.
	 */
	public MaxTransactionsBlockValidator(final int maxTransactionsPerBlock) {
		this.maxTransactionsPerBlock = maxTransactionsPerBlock;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final long numTransactions = BlockExtensions.streamDefault(block).count();
		return numTransactions <= this.maxTransactionsPerBlock
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
	}
}
