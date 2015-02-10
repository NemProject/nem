package org.nem.nis.validators.unconfirmed;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A TransferTransactionValidator implementation that ensures that a new importance transfer transactions
 * does not conflict with any known transactions.
 * This is used by UnconfirmedTransactions and is not a default validator.
 */
public class NonConflictingImportanceTransferTransactionValidator implements SingleTransactionValidator {
	private final Supplier<Stream<Transaction>> transactionsSupplier;

	/**
	 * Creates a new validator.
	 *
	 * @param transactionsSupplier A supplier that returns the known transactions.
	 */
	public NonConflictingImportanceTransferTransactionValidator(final Supplier<Stream<Transaction>> transactionsSupplier) {
		this.transactionsSupplier = transactionsSupplier;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.IMPORTANCE_TRANSFER != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return this.validate((ImportanceTransferTransaction)transaction);
	}

	private ValidationResult validate(final ImportanceTransferTransaction transaction) {
		final boolean anyTransactionConflicts = this.transactionsSupplier.get()
				.filter(t -> TransactionTypes.IMPORTANCE_TRANSFER == t.getType())
				.filter(t -> t != transaction)
				.anyMatch(t -> areConflicting(transaction, (ImportanceTransferTransaction)t));

		return anyTransactionConflicts
				? ValidationResult.FAILURE_CONFLICTING_IMPORTANCE_TRANSFER
				: ValidationResult.SUCCESS;
	}

	private static boolean areConflicting(final ImportanceTransferTransaction lhs, final ImportanceTransferTransaction rhs) {
		// don't allow pending unconfirmed transactions to have the same signer (source) or remote
		return lhs.getSigner().equals(rhs.getSigner()) || lhs.getRemote().equals(rhs.getRemote());
	}
}