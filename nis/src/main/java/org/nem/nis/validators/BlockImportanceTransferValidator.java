package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A batch transaction validator that ensures all importance transactions are non-conflicting.
 */
public class BlockImportanceTransferValidator implements BlockValidator {
	private static final Logger LOGGER = Logger.getLogger(BlockImportanceTransferValidator.class.getName());

	/**
	 * Creates a new validator.
	 */
	public BlockImportanceTransferValidator() {
	}

	@Override
	public ValidationResult validate(final Block block) {
		if (block.getTransactions().isEmpty()) {
			return ValidationResult.SUCCESS;
		}

		final List<Transaction> transactions = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
				.collect(Collectors.toList());
		if (transactions.isEmpty()) {
			return ValidationResult.SUCCESS;
		}
		final SingleTransactionValidator validator = new NonConflictingImportanceTransferTransactionValidator(() -> transactions);
		final ValidationResult result = ValidationResult.aggregate(transactions.stream().map(t -> validator.validate(t)).iterator());
		if (result.isFailure()) {
			return result;
		}
		return ValidationResult.SUCCESS;
	}
}