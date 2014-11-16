package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A block transaction validator that ensures all importance transactions within a block are non-conflicting.
 */
public class BlockImportanceTransferValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		final List<Transaction> transactions = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
				.collect(Collectors.toList());

		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> transactions);
		return ValidationResult.aggregate(transactions.stream().map(t -> validator.validate(t)).iterator());
	}
}