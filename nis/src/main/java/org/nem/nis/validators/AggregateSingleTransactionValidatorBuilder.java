package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

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
		this.validators.add((transaction, context) -> validator.validate(Arrays.asList(new TransactionsContextPair(transaction, context))));
	}

	/**
	 * Builds the aggregate validator.
	 *
	 * @return the aggregate validator.
	 */
	public SingleTransactionValidator build() {
		return new AggregateSingleTransactionValidator(this.validators);
	}

	private static class AggregateSingleTransactionValidator implements SingleTransactionValidator {
		private final List<SingleTransactionValidator> validators;

		public AggregateSingleTransactionValidator(final List<SingleTransactionValidator> validators) {
			this.validators = validators;
		}

		@Override
		public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
			// TODO 20141204 G-J: java documentation does not make this clear to me, is .stream() guaranteed to be executed sequentially?
			// there's following statement in docs:
			// "The stream implementations in the JDK create serial streams unless parallelism is explicitly requested."
			return ValidationResult.aggregate(this.validators.stream()
					.map(validator -> validator.validate(transaction, context))
					.iterator());
		}
	}
}
