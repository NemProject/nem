package org.nem.nis.validators;

import org.nem.core.model.HashCache;
import org.nem.core.time.TimeProvider;
import org.nem.nis.poi.*;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {
	private final TimeProvider timeProvider;
	private final PoiOptions poiOptions;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 * @param poiOptions The poi options.
	 */
	public TransactionValidatorFactory(
			final TimeProvider timeProvider,
			final PoiOptions poiOptions) {
		this.timeProvider = timeProvider;
		this.poiOptions = poiOptions;
	}

	/**
	 * Creates a transaction validator that contains both single and batch validators.
	 *
	 * @param poiFacade The poi facade.
	 * @param transactionHashCache The transaction hash cache.
	 * @return The validator.
	 */
	public SingleTransactionValidator create(final PoiFacade poiFacade, final HashCache transactionHashCache) {
		final AggregateSingleTransactionValidatorBuilder builder = this.createSingleBuilder(poiFacade);
		builder.add(new BatchUniqueHashTransactionValidator(transactionHashCache));
		return builder.build();
	}

	/**
	 * Creates a transaction validator that only contains single validators.
	 *
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public SingleTransactionValidator createSingle(final PoiFacade poiFacade) {
		return this.createSingleBuilder(poiFacade).build();
	}

	/**
	 * Creates a transaction validator that only contains batch validators.
	 *
	 * @param transactionHashCache The transaction hash cache.
	 * @return The validator.
	 */
	public BatchTransactionValidator createBatch(final HashCache transactionHashCache) {
		return new BatchUniqueHashTransactionValidator(transactionHashCache);
	}

	private AggregateSingleTransactionValidatorBuilder createSingleBuilder(final PoiFacade poiFacade) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		builder.add(new UniversalTransactionValidator());
		builder.add(new NonFutureEntityValidator(this.timeProvider));
		builder.add(new TransferTransactionValidator());
		builder.add(new ImportanceTransferTransactionValidator(poiFacade, this.poiOptions.getMinHarvesterBalance()));
		return builder;
	}
}
