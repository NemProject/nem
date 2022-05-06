package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.validators.BlockValidator;

/**
 * A block transaction validator that ensures all transactions have a valid deadline.
 */
public class TransactionDeadlineBlockValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		return ValidationResult.aggregate(block.getTransactions().stream()
				.map(t -> t.getDeadline().compareTo(block.getTimeStamp()) >= 0
						? ValidationResult.SUCCESS
						: ValidationResult.FAILURE_PAST_DEADLINE)
				.iterator());
	}
}
