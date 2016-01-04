package org.nem.core.model;

import java.util.Arrays;

/**
 * Class that encapsulates block chain configuration data.
 */
public interface BlockChainConfiguration {

	//region transactions per sync

	/**
	 * Gets the minimum number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The minimum number of transactions.
	 */
	default int getMinTransactionsPerSyncAttempt() {
		return this.getMaxTransactionsPerBlock();
	}

	/**
	 * Gets the default number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The default number of transactions.
	 */
	default int getDefaultTransactionsPerSyncAttempt() {
		return this.getMaxTransactionsPerSyncAttempt() / 2;
	}

	/**
	 * Gets the maximum number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The maximum number of transactions.
	 */
	int getMaxTransactionsPerSyncAttempt();

	//endregion

	//region blocks per sync

	/**
	 * Gets the default number of blocks transferred during syncing.
	 *
	 * @return The default number of blocks transferred during syncing.
	 */
	default int getDefaultBlocksPerSyncAttempt() {
		return this.getMaxBlocksPerSyncAttempt() / 4;
	}

	/**
	 * Gets the maximum number of blocks transferred during syncing.
	 *
	 * @return The maximum number of blocks transferred during syncing.
	 */
	default int getMaxBlocksPerSyncAttempt() {
		return this.getBlockChainRewriteLimit() + 40;
	}

	//endregion

	/**
	 * Gets the maximum number of transactions allowed in a single block.
	 *
	 * @return The maximum number of transactions.
	 */
	int getMaxTransactionsPerBlock();

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

	//region getEstimatedBlocksPerX

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

	//endregion

	//region block chain features

	/**
	 * Gets the block chain features.
	 *
	 * @return The block chain features.
	 */
	BlockChainFeature[] getBlockChainFeatures();

	/**
	 * Gets a value indicating whether or not the block chain supports the specified feature.
	 *
	 * @return true if the block chain supports the specified feature.
	 */
	default boolean isBlockChainFeatureSupported(final BlockChainFeature feature) {
		return Arrays.stream(this.getBlockChainFeatures()).anyMatch(f -> f == feature);
	}

	//endregion
}
