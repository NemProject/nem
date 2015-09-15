package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.validators.block.*;

/**
 * Factory for creating BlockValidator objects.
 */
public class BlockValidatorFactory {
	private final TimeProvider timeProvider;
	private final int maxTransactionsPerBlock;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 * @param maxTransactionsPerBlock The maximum number of transactions per block.
	 */
	public BlockValidatorFactory(final TimeProvider timeProvider, final int maxTransactionsPerBlock) {
		this.timeProvider = timeProvider;
		this.maxTransactionsPerBlock = maxTransactionsPerBlock;
	}

	/**
	 * Creates a block validator.
	 *
	 * @param nisCache The NIS cache.
	 * @return The validator.
	 */
	public BlockValidator create(final ReadOnlyNisCache nisCache) {
		final AggregateBlockValidatorBuilder builder = new AggregateBlockValidatorBuilder();
		builder.add(new BlockNonFutureEntityValidator(this.timeProvider));
		builder.add(new TransactionDeadlineBlockValidator());
		builder.add(new EligibleSignerBlockValidator(nisCache.getAccountStateCache()));
		builder.add(new MaxTransactionsBlockValidator(this.maxTransactionsPerBlock));
		builder.add(new NoSelfSignedTransactionsBlockValidator(nisCache.getAccountStateCache()));
		builder.add(new BlockUniqueHashTransactionValidator(nisCache.getTransactionHashCache()));
		builder.add(new BlockNetworkValidator());
		builder.add(new BlockMosaicDefinitionCreationValidator());
		builder.add(new VersionBlockValidator());
		builder.add(this.createTransactionOnly(nisCache));
		return builder.build();
	}

	/**
	 * Creates a block validator that only includes block transaction validators.
	 *
	 * @param nisCache The NIS cache.
	 * @return The validator.
	 */
	public BlockValidator createTransactionOnly(final ReadOnlyNisCache nisCache) {
		final AggregateBlockValidatorBuilder builder = new AggregateBlockValidatorBuilder();
		builder.add(new BlockMultisigAggregateModificationValidator());
		return builder.build();
	}
}