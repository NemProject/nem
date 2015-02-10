package org.nem.nis.validators;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeProvider;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.poi.PoiOptions;
import org.nem.nis.validators.transaction.*;
import org.nem.nis.validators.unconfirmed.*;

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
	 * Creates a transaction validator that only contains single validators.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The validator.
	 */
	public SingleTransactionValidator createSingle(final ReadOnlyAccountStateCache accountStateCache) {
		return this.createSingleBuilder(accountStateCache).build();
	}

	/**
	 * Creates a transaction validator builder that is only initialized with single validators and can be used
	 * for verifying blocks.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createSingleBuilder(final ReadOnlyAccountStateCache accountStateCache) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		this.visitSingleSubValidators(builder::add, accountStateCache, true);
		return builder;
	}

	/**
	 * Creates a transaction validator builder that is only initialized with single validators and should be used
	 * for verifying transactions outside of blocks (it excludes validators that check for "incomplete" transactions).
	 *
	 * @param accountStateCache The account state cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createIncompleteSingleBuilder(final ReadOnlyAccountStateCache accountStateCache) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		this.visitSingleSubValidators(builder::add, accountStateCache, false);
		return builder;
	}

	/**
	 * Creates a transaction validator that only contains batch validators.
	 *
	 * @param transactionHashCache The transaction hash cache.
	 * @return The validator.
	 */
	public BatchTransactionValidator createBatch(final ReadOnlyHashCache transactionHashCache) {
		final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();
		this.visitBatchSubValidators(builder::add, transactionHashCache);
		return builder.build();
	}

	/**
	 * Visits all sub validators that comprise the validator returned by createSingle.
	 *
	 * @param visitor The visitor.
	 * @param accountStateCache The account state cache.
	 * @param includeAllValidators true if all validators should be included; false if transactions should not be validated for completeness.
	 */
	public void visitSingleSubValidators(
			final Consumer<SingleTransactionValidator> visitor,
			final ReadOnlyAccountStateCache accountStateCache,
			final boolean includeAllValidators) {
		visitor.accept(new UniversalTransactionValidator());
		visitor.accept(new TransactionNonFutureEntityValidator(this.timeProvider));
		visitor.accept(new NemesisSinkValidator());

		visitor.accept(new TransferTransactionValidator());
		visitor.accept(new ImportanceTransferTransactionValidator(accountStateCache, this.poiOptions.getMinHarvesterBalance()));
		visitor.accept(
				new BlockHeightSingleTransactionValidatorDecorator(
						new BlockHeight(BlockMarkerConstants.BETA_REMOTE_VALIDATION_FORK),
						new RemoteNonOperationalValidator(accountStateCache)));

		visitor.accept(new MultisigNonOperationalValidator(accountStateCache));
		visitor.accept(new MultisigTransactionSignerValidator(accountStateCache));
		visitor.accept(new MultisigAggregateModificationTransactionValidator(accountStateCache));
		visitor.accept(new MaxCosignatoryValidator(accountStateCache));

		if (includeAllValidators) {
			visitor.accept(new MultisigSignaturesPresentValidator(accountStateCache));
		}
	}

	/**
	 * Visits all sub validators that comprise the validator returned by createBatch.
	 *
	 * @param visitor The visitor.
	 * @param transactionHashCache The cache of transaction hashes.
	 */
	public void visitBatchSubValidators(
			final Consumer<BatchTransactionValidator> visitor,
			final ReadOnlyHashCache transactionHashCache) {
		visitor.accept(new BatchUniqueHashTransactionValidator(transactionHashCache));
	}
}
