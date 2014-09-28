package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * A TransferTransactionValidator implementation that ensures that a new importance transfer transactions
 * does not conflict with any known transactions.
 * This is used by UnconfirmedTransactions and is not a default validator.
 */
public class NonConflictingImportanceTransferTransactionValidator implements TransactionValidator {
	private final Supplier<Collection<Transaction>> transactionsSupplier;

	/**
	 * Creates a new validator.
	 *
	 * @param transactionsSupplier A supplier that returns the known transactions.
	 */
	public NonConflictingImportanceTransferTransactionValidator(final Supplier<Collection<Transaction>> transactionsSupplier) {
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
		for (final Transaction existingTransaction : this.transactionsSupplier.get()) {
			if (existingTransaction.getType() != TransactionTypes.IMPORTANCE_TRANSFER) {
				continue;
			}

			if (transaction == existingTransaction) {
				continue;
			}

			if (areConflicting(transaction, (ImportanceTransferTransaction)existingTransaction)) {
				return ValidationResult.FAILURE_ENTITY_UNUSABLE;
			}
		}

		return ValidationResult.SUCCESS;
	}

	private static boolean areConflicting(final ImportanceTransferTransaction lhs, final ImportanceTransferTransaction rhs) {
		// don't allow pending unconfirmed transactions to have the same signer (source) or remote
		return lhs.getSigner().equals(rhs.getSigner()) || lhs.getRemote().equals(rhs.getRemote());
	}
}