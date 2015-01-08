package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.*;

/**
 * A block validator that ensures a block does not have more than the maximum number of transactions.
 */
public class MaxTransactionsBlockValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		return block.getTransactions().size() <= BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK(block.getHeight())
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_TOO_MANY_TRANSACTIONS;
	}
}
