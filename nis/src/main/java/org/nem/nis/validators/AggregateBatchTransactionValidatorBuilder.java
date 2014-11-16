package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

// TODO 20141116 J-J: obviously we need tests :)

/**
 * Builder for building an aggregate BatchTransactionValidator.
 */
public class AggregateBatchTransactionValidatorBuilder {
	private final List<BatchTransactionValidator> validators = new ArrayList<>();

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final BatchTransactionValidator validator) {
		this.validators.add(validator);
	}

	/**
	 * Builds the aggregate validator.
	 *
	 * @return the aggregate validator.
	 */
	public BatchTransactionValidator build() {
		return new AggregateBlockValidator(this.validators);
	}

	private static class AggregateBlockValidator implements BatchTransactionValidator {
		private final List<BatchTransactionValidator> validators;

		public AggregateBlockValidator(final List<BatchTransactionValidator> validators) {
			this.validators = validators;
		}

		@Override
		public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
			final Iterator<ValidationResult> resultIterator = this.validators.stream()
					.map(validator -> validator.validate(groupedTransactions))
					.iterator();
			return ValidationResult.aggregate(resultIterator);
		}
	}
}
