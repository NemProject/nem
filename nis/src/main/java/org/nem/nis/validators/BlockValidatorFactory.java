package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.validators.block.*;
import org.nem.nis.ForkConfiguration;

/**
 * Factory for creating BlockValidator objects.
 */
public class BlockValidatorFactory {
	private final TimeProvider timeProvider;
	private final ForkConfiguration forkConfiguration;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 * @param forkConfiguration The fork configuration.
	 */
	public BlockValidatorFactory(final TimeProvider timeProvider, final ForkConfiguration forkConfiguration) {
		this.timeProvider = timeProvider;
		this.forkConfiguration = forkConfiguration;
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
		builder.add(new MaxTransactionsBlockValidator());
		builder.add(new NoSelfSignedTransactionsBlockValidator(nisCache.getAccountStateCache()));
		builder.add(new BlockUniqueHashTransactionValidator(nisCache.getTransactionHashCache()));
		builder.add(new BlockNetworkValidator());
		builder.add(new VersionBlockValidator());
		builder.add(new TreasuryReissuanceForkTransactionBlockValidator(this.forkConfiguration));
		builder.add(this.createTransactionOnly());
		return builder.build();
	}

	/**
	 * Creates a block validator that only includes block transaction validators.
	 *
	 * @return The validator.
	 */
	public BlockValidator createTransactionOnly() {
		final AggregateBlockValidatorBuilder builder = new AggregateBlockValidatorBuilder();
		builder.add(new BlockMultisigAggregateModificationValidator());
		builder.add(new BlockMosaicDefinitionCreationValidator());
		return builder.build();
	}
}
