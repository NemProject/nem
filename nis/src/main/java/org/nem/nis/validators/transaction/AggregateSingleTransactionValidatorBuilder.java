package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builder for building an aggregate SingleTransactionValidator.
 */
public class AggregateSingleTransactionValidatorBuilder {
	private final List<SingleTransactionValidator> validators = new ArrayList<>();

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final SingleTransactionValidator validator) {
		this.validators.add(validator);
	}

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final BatchTransactionValidator validator) {
		this.validators.add(
				(transaction, context) -> validator.validate(Collections.singletonList(new TransactionsContextPair(transaction, context))));
	}

	/**
	 * Builds the aggregate validator.
	 *
	 * @return the aggregate validator.
	 */
	public SingleTransactionValidator build() {
		return new ChildAwareSingleTransactionValidator(new AggregateSingleTransactionValidator(this.validators));
	}

	private static class AggregateSingleTransactionValidator implements SingleTransactionValidator {
		private final List<SingleTransactionValidator> validators;

		public AggregateSingleTransactionValidator(final List<SingleTransactionValidator> validators) {
			this.validators = validators;
		}

		@Override
		public String getName() {
			return this.validators.stream().map(NamedValidator::getName).collect(Collectors.joining(","));
		}

		@Override
		public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
			return ValidationResult
					.aggregate(this.validators.stream().map(validator -> validator.validate(transaction, context)).iterator());
		}
	}
}
