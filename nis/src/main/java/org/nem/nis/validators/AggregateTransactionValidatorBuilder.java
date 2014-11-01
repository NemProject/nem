package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.*;

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
	public void add(final SingleTransactionValidator validator) {
		//this.validators.add(new TransactionValidator() {groupedTransactions -> {
		//	final Iterator<ValidationResult> resultIterator =
		//			groupedTransactions.stream()
		//					.map(pair -> ValidationResult.aggregate(
		//							pair.getTransactions().stream()
		//									.map(transaction -> validator.validate(transaction, pair.getContext()))
		//									.iterator()))
		//					.iterator();
		//
		//	return ValidationResult.aggregate(resultIterator);
		//});
	}

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final BatchTransactionValidator validator) {
		this.validators.add(new TransactionValidator() {
			@Override
			public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
				return null;
			}

			@Override
			public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
				return null;
			}
		});
	}

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
		public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
			return ValidationResult.aggregate(this.validators.stream()
					.map(validator -> validator.validate(groupedTransactions))
					.iterator());
		}

		@Override
		public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
			return ValidationResult.aggregate(this.validators.stream()
					.map(validator -> validator.validate(transaction, context))
					.iterator());
		}
	}
}
