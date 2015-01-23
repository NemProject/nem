package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A single transaction used (only) by unconfirmed transactions that validates:
 * - a multisig modification does not conflict with any known pending transactions
 */
public class NonConflictingMultisigAggregateModificationValidator implements SingleTransactionValidator {
	private final Supplier<Collection<Transaction>> transactionsSupplier;

	/**
	 * Creates a new validator.
	 *
	 * @param transactionsSupplier A supplier that returns the known transactions.
	 */
	public NonConflictingMultisigAggregateModificationValidator(final Supplier<Collection<Transaction>> transactionsSupplier) {
		this.transactionsSupplier = transactionsSupplier;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		final Set<Account> modificationSigners = this.transactionsSupplier.get().stream()
				.filter(t -> TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION == t.getType())
				.map(t -> t.getSigner())
				.collect(Collectors.toSet());

		return modificationSigners.contains(transaction.getSigner())
				? ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION
				: ValidationResult.SUCCESS;
	}
}
