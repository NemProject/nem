package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

/**
 * Builder for building an aggregate AggregateValidatorBuilder.
 */
public class AggregateTransactionValidatorBuilder {
	private final List<TransactionValidator> validators = new ArrayList<>();

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final TransactionValidator validator) {
		this.validators.add(validator);
	}

	/**
	 * Builds the aggregate validator.
	 *
	 * @return the aggregate validator.
	 */
	public TransactionValidator build() {
		return new AggregateTransactionValidator(this.validators);
	}

	private static class AggregateTransactionValidator implements TransactionValidator {
		private final List<TransactionValidator> validators;

		public AggregateTransactionValidator(final List<TransactionValidator> validators) {
			this.validators = validators;
		}

		@Override
		public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
			boolean isNeutral = false;
			for (final TransactionValidator validator : this.validators) {
				final ValidationResult result = validator.validate(transaction, context);
				if (ValidationResult.NEUTRAL == result) {
					isNeutral = true;
				} else if (ValidationResult.SUCCESS != result) {
					return result;
				}
			}

			return isNeutral ? ValidationResult.NEUTRAL : ValidationResult.SUCCESS;
		}
	}
}
