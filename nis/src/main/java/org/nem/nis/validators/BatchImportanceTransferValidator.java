package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A batch transaction validator that ensures all importance transactions are non-conflicting.
 */
public class BatchImportanceTransferValidator implements BatchTransactionValidator, Supplier<Collection<Transaction>> {
	private static final Logger LOGGER = Logger.getLogger(BatchImportanceTransferValidator.class.getName());
	private final NonConflictingImportanceTransferTransactionValidator importanceTransferTransactionValidator;
	private Collection<Transaction> transactions;

	/**
	 * Creates a new validator.
	 */
	public BatchImportanceTransferValidator() {
		// TODO: how to do this in a nice manner?
		this.importanceTransferTransactionValidator = new NonConflictingImportanceTransferTransactionValidator(this);
	}

	@Override
	public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
		if (groupedTransactions.isEmpty()) {
			return ValidationResult.SUCCESS;
		}
		this.transactions = groupedTransactions.get(0).getTransactions().stream()
		                                       .filter(t -> t.getType() == TransactionTypes.IMPORTANCE_TRANSFER)
		                                       .collect(Collectors.toList());
		if (this.transactions.isEmpty()) {
			return ValidationResult.SUCCESS;
		}
		final boolean result = this.transactions.stream().anyMatch(t -> ValidationResult.SUCCESS != this.importanceTransferTransactionValidator.validate(t));
		return result ? ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER : ValidationResult.SUCCESS;
	}

	@Override
	public Collection<Transaction> get() {
		return transactions;
	}
}