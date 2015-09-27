package org.nem.core.model;

/**
 * Class that encapsulates block chain configuration data.
 */
public interface BlockChainConfiguration {

	/**
	 * Gets the maximum number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The maximum number of transactions.
	 */
	int getMaxTransactionsPerSyncAttempt();

	/**
	 * Gets the default number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The default number of transactions.
	 */
	default int getDefaultMaxTransactionsPerSyncAttempt() {
		return this.getMaxTransactionsPerSyncAttempt() / 2;
	}

	/**
	 * Gets the minimum number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The minimum number of transactions.
	 */
	default int getMinTransactionsPerSyncAttempt() {
		return this.getMaxTransactionsPerBlock();
	}

	/**
	 * Gets the maximum number of transactions allowed in a single block.
	 *
	 * @return The maximum number of transactions.
	 */
	int getMaxTransactionsPerBlock();

	/**
	 * Gets the maximum number of blocks transferred during syncing.
	 *
	 * @return The maximum number of blocks transferred during syncing.
	 */
	default int getSyncBlockLimit() {
		return this.getBlockChainRewriteLimit() + 40;
	}

	/**
	 * Gets the target time between two blocks.
	 *
	 * @return The target time between two blocks.
	 */
	int getBlockGenerationTargetTime();

	/**
	 * Gets the block chain rewrite limit.
	 *
	 * @return The block chain rewrite limit.
	 */
	int getBlockChainRewriteLimit();

	/**
	 * Gets the estimated number of blocks per day.
	 *
	 * @return The estimated number of blocks per day.
	 */
	default int getEstimatedBlocksPerDay() {
		return 60 * 60 * 24 / this.getBlockGenerationTargetTime();
	}

	/**
	 * Gets the estimated number of blocks per month.
	 *
	 * @return The estimated number of blocks per month.
	 */
	default int getEstimatedBlocksPerMonth() {
		return this.getEstimatedBlocksPerDay() * 30;
	}

	/**
	 * Gets the estimated number of blocks per year.
	 *
	 * @return The estimated number of blocks per year.
	 */
	default int getEstimatedBlocksPerYear() {
		return this.getEstimatedBlocksPerDay() * 365;
	}

	/**
	 * Gets a value indicating whether or not the block chain supports the specified feature.
	 *
	 * @return true if the block chain supports the specified feature.
	 */
	boolean isBlockChainFeatureSupported(final BlockChainFeature feature);
}
