package org.nem.nis.validators.unconfirmed;

import org.nem.core.model.ValidationResult;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.Collectors;

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
		return new AggregateBatchTransactionValidator(this.validators);
	}

	private static class AggregateBatchTransactionValidator implements BatchTransactionValidator {
		private final List<BatchTransactionValidator> validators;

		public AggregateBatchTransactionValidator(final List<BatchTransactionValidator> validators) {
			this.validators = validators;
		}

		@Override
		public String getName() {
			return this.validators.stream().map(NamedValidator::getName).collect(Collectors.joining(","));
		}

		@Override
		public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
			return ValidationResult
					.aggregate(this.validators.stream().map(validator -> validator.validate(groupedTransactions)).iterator());
		}
	}
}
