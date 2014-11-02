package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

/**
 * Builder for building an aggregate AggregateValidatorBuilder.
 */
public class AggregateTransactionValidatorBuilder {
	// TODO 20141102 J-*: initially i thought i could be clever and run all transactions as batch;
	// > unfortunately, that didn't work because we "execute" blocks as we validate transactions
	// > thus, some validation logic is dependent on block execution (i really need to add a test for that)
	// > so, i need to clean this up a bit
	// > one thing to note is that the db-check logic needs to be able to run as a SingleTransactionValidator
	// > so we don't add multiple unconfirmed transactions with the same hash (this is broken in master)
	private final List<TransactionValidator> validators = new ArrayList<>();
	private final List<SingleTransactionValidator> singleValidators = new ArrayList<>();
	private final List<BatchTransactionValidator> batchValidators = new ArrayList<>();

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final SingleTransactionValidator validator) {
		this.singleValidators.add(validator);
		this.validators.add(new TransactionValidator() {
			@Override
			public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
				final Iterator<ValidationResult> resultIterator = groupedTransactions.stream()
						.map(pair -> {
							final Iterator<ValidationResult> pairResultIterator = pair.getTransactions().stream()
									.map(transaction -> this.validate(transaction, pair.getContext()))
									.iterator();
							return ValidationResult.aggregate(pairResultIterator);
						})
						.iterator();

				return ValidationResult.aggregate(resultIterator);
			}

			@Override
			public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
				return validator.validate(transaction, context);
			}
		});
	}

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final BatchTransactionValidator validator) {
		this.batchValidators.add(validator);
		this.validators.add(new TransactionValidator() {
			@Override
			public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
				return validator.validate(groupedTransactions);
			}

			@Override
			public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
				return this.validate(Arrays.asList(new TransactionsContextPair(transaction, context)));
			}
		});
	}

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final TransactionValidator validator) {
		this.singleValidators.add(validator);
		this.batchValidators.add(validator);
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

	/**
	 * Builds the aggregate validator.
	 *
	 * @return the aggregate validator.
	 */
	public TransactionValidator buildBatchOptimized() {
		return new SplitAggregateTransactionValidator(this.singleValidators, this.batchValidators);
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

	private static class SplitAggregateTransactionValidator implements TransactionValidator {
		private final List<SingleTransactionValidator> singleValidators;
		private final List<BatchTransactionValidator> batchValidators;

		public SplitAggregateTransactionValidator(
				final List<SingleTransactionValidator> singleValidators,
				final List<BatchTransactionValidator> batchValidators) {
			this.singleValidators = singleValidators;
			this.batchValidators = batchValidators;
		}

		@Override
		public ValidationResult validate(final List<TransactionsContextPair> groupedTransactions) {
			return ValidationResult.aggregate(this.batchValidators.stream()
					.map(validator -> validator.validate(groupedTransactions))
					.iterator());
		}

		@Override
		public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
			return ValidationResult.aggregate(this.singleValidators.stream()
					.map(validator -> validator.validate(transaction, context))
					.iterator());
		}
	}
}
