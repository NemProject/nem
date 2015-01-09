package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A block transaction validator that ensures all importance transactions within a block are non-conflicting.
 */
public class BlockImportanceTransferValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		final List<Transaction> importanceTransfers = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
				.collect(Collectors.toList());

		// most blocks don't contain importance transfer, so it has sense to do short circuit
		if (importanceTransfers.isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		// it is ok to pass in a validation context with a null debit predicate because the
		// NonConflictingImportanceTransferTransactionValidator does not require debit checks
		// and doing so avoids a dependency on the poi facade
		final ValidationContext validationContext = new ValidationContext(null);
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> importanceTransfers);
		return ValidationResult.aggregate(importanceTransfers.stream().map(t -> validator.validate(t, validationContext)).iterator());
	}
}