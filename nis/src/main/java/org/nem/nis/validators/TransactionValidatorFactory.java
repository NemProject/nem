package org.nem.nis.validators;

import org.nem.core.model.HashCache;
import org.nem.core.time.TimeProvider;
import org.nem.nis.NisCache;
import org.nem.nis.poi.*;

import java.util.function.Consumer;

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
	 * @param nisCache The NIS cache.
	 * @return The validator.
	 */
	public SingleTransactionValidator create(final NisCache nisCache) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		this.visitSingleSubValidators(builder::add, nisCache.getPoiFacade());
		this.visitBatchSubValidators(builder::add, nisCache.getTransactionHashCache());
		return builder.build();
	}

	/**
	 * Creates a transaction validator that only contains single validators.
	 *
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public SingleTransactionValidator createSingle(final PoiFacade poiFacade) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		this.visitSingleSubValidators(builder::add, poiFacade);
		return builder.build();
	}

	/**
	 * Creates a transaction validator that only contains batch validators.
	 *
	 * @param transactionHashCache The transaction hash cache.
	 * @return The validator.
	 */
	public BatchTransactionValidator createBatch(final HashCache transactionHashCache) {
		final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();
		this.visitBatchSubValidators(builder::add, transactionHashCache);
		return builder.build();
	}

	/**
	 * Visits all sub validators that comprise the validator returned by createSingle.
	 *
	 * @param visitor The visitor.
	 * @param poiFacade The poi facade.
	 */
	public void visitSingleSubValidators(final Consumer<SingleTransactionValidator> visitor, final PoiFacade poiFacade) {
		visitor.accept(new UniversalTransactionValidator());
		visitor.accept(new NonFutureEntityValidator(this.timeProvider));
		visitor.accept(new TransferTransactionValidator());
		visitor.accept(new ImportanceTransferTransactionValidator(poiFacade, this.poiOptions.getMinHarvesterBalance()));
		visitor.accept(new ImportanceTransferTransactionValidator(poiFacade, this.poiOptions.getMinHarvesterBalance()));
	}

	/**
	 * Visits all sub validators that comprise the validator returned by createBatch.
	 *
	 * @param visitor The visitor.
	 * @param transactionHashCache The transaction hash cache.
	 */
	public void visitBatchSubValidators(final Consumer<BatchTransactionValidator> visitor, final HashCache transactionHashCache) {
		visitor.accept(new BatchUniqueHashTransactionValidator(transactionHashCache));
	}
}
