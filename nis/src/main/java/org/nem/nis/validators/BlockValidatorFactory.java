package org.nem.nis.validators;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeProvider;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.validators.block.*;

/**
 * Factory for creating BlockValidator objects.
 */
public class BlockValidatorFactory {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 */
	public BlockValidatorFactory(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
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