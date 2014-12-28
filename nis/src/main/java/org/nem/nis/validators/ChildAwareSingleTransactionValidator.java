package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

/**
 * SingleTransactionValidator decorator that knows how to validate child transactions.
 */
public class ChildAwareSingleTransactionValidator implements SingleTransactionValidator {
	private final SingleTransactionValidator validator;

	/**
	 * Creates a child-aware single transaction validator.
	 *
	 * @param validator The decorated validator.
	 */
	public ChildAwareSingleTransactionValidator(final SingleTransactionValidator validator) {
		this.validator = validator;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final List<Transaction> transactions = new ArrayList<>();
		transactions.add(transaction);
		transactions.addAll(transaction.getChildTransactions());
		return ValidationResult.aggregate(
				transactions.stream()
						.map(t -> this.validator.validate(t, context))
						.iterator());
	}
}
