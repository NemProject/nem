package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

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
	public String getName() {
		return this.validator.getName();
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return ValidationResult
				.aggregate(TransactionExtensions.streamDefault(transaction).map(t -> this.validator.validate(t, context)).iterator());
	}
}
