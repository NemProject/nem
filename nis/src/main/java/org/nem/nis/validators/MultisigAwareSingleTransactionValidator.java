package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

/**
 * SingleTransactionValidator decorator that knows how to validate multisig (aggregate) transactions.
 */
public class MultisigAwareSingleTransactionValidator implements SingleTransactionValidator {
	private final SingleTransactionValidator validator;

	/**
	 * Creates a multisig-aware single transaction validator.
	 *
	 * @param validator The decorated validator.
	 */
	public MultisigAwareSingleTransactionValidator(final SingleTransactionValidator validator) {
		this.validator = validator;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final List<Transaction> transactions = new ArrayList<>();
		transactions.add(transaction);

		if (TransactionTypes.MULTISIG == transaction.getType()) {
			transactions.add(((MultisigTransaction)transaction).getOtherTransaction());
		}

		return ValidationResult.aggregate(
				transactions.stream()
						.map(t -> this.validator.validate(t, context))
						.iterator());
	}
}
