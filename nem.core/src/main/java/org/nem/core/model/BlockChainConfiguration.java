package org.nem.core.model;

import org.nem.core.utils.MustBe;

import java.util.Arrays;

/**
 * Class that encapsulates block chain configuration data.
 */
public class BlockChainConfiguration {
	private final int maxTransactionsPerSyncAttempt;
	private final int maxTransactionsPerBlock;
	private final int blockGenerationTargetTime;
	private final int blockChainRewriteLimit;
	private final BlockChainFeature[] blockChainFeatures;

	/**
	 * Creates a new clock chain configuration.
	 *
	 * @param maxTransactionsPerSyncAttempt The maximum number of transactions that a remote peer supplies in a chain part.
	 * @param maxTransactionsPerBlock The maximum number of transactions allowed in a single block.
	 * @param blockGenerationTargetTime The target time between two blocks in seconds.
	 * @param blockChainRewriteLimit The block chain rewrite limit.
	 */
	public BlockChainConfiguration(
			final int maxTransactionsPerSyncAttempt,
			final int maxTransactionsPerBlock,
			final int blockGenerationTargetTime,
			final int blockChainRewriteLimit) {
		this(
				maxTransactionsPerSyncAttempt,
				maxTransactionsPerBlock,
				blockGenerationTargetTime,
				blockChainRewriteLimit,
				BlockChainFeature.explode(BlockChainFeature.PROOF_OF_IMPORTANCE.value()));
	}

	/**
	 * Creates a new clock chain configuration.
	 *
	 * @param maxTransactionsPerSyncAttempt The maximum number of transactions that a remote peer supplies in a chain part.
	 * @param maxTransactionsPerBlock The maximum number of transactions allowed in a single block.
	 * @param blockGenerationTargetTime The target time between two blocks in seconds.
	 * @param blockChainRewriteLimit The block chain rewrite limit.
	 * @param blockChainFeatures The block chain features.
	 */
	public BlockChainConfiguration(
			final int maxTransactionsPerSyncAttempt,
			final int maxTransactionsPerBlock,
			final int blockGenerationTargetTime,
			final int blockChainRewriteLimit,
			final BlockChainFeature[] blockChainFeatures) {
		this.maxTransactionsPerSyncAttempt = maxTransactionsPerSyncAttempt;
		this.maxTransactionsPerBlock = maxTransactionsPerBlock;
		this.blockGenerationTargetTime = blockGenerationTargetTime;
		this.blockChainRewriteLimit = blockChainRewriteLimit;
		this.blockChainFeatures = blockChainFeatures;
		MustBe.inRange(
				this.maxTransactionsPerSyncAttempt,
				"max transactions per sync attempt",
				this.maxTransactionsPerBlock,
				this.maxTransactionsPerBlock * this.blockChainRewriteLimit);
		MustBe.inRange(this.maxTransactionsPerBlock, "max transactions per block", 1, 10_000);
		MustBe.inRange(this.blockGenerationTargetTime, "block generation target time", 10, 86_400);
		MustBe.inRange(this.blockChainRewriteLimit, "block chain rewrite limit", 10, this.getEstimatedBlocksPerDay());
		MustBe.notNull(this.blockChainFeatures, "block chain features");
	}

	/**
	 * Gets the maximum number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The maximum number of transactions.
	 */
	public int getMaxTransactionsPerSyncAttempt() {
		return this.maxTransactionsPerSyncAttempt;
	}

	/**
	 * Gets the default number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The default number of transactions.
	 */
	public int getDefaultMaxTransactionsPerSyncAttempt() {
		return this.maxTransactionsPerSyncAttempt / 2;
	}

	/**
	 * Gets the minimum number of transactions that a remote peer supplies in a chain part.
	 *
	 * @return The minimum number of transactions.
	 */
	public int getMinTransactionsPerSyncAttempt() {
		return this.maxTransactionsPerBlock;
	}

	/**
	 * Gets the maximum number of transactions allowed in a single block.
	 *
	 * @return The maximum number of transactions.
	 */
	public int getMaxTransactionsPerBlock() {
		return this.maxTransactionsPerBlock;
	}

	/**
	 * Gets the maximum number of blocks transferred during syncing.
	 *
	 * @return The maximum number of blocks transferred during syncing.
	 */
	public int getSyncBlockLimit() {
		return this.blockChainRewriteLimit + 40;
	}

	/**
	 * Gets the target time between two blocks.
	 *
	 * @return The target time between two blocks.
	 */
	public int getBlockGenerationTargetTime() {
		return this.blockGenerationTargetTime;
	}

	/**
	 * Gets the block chain rewrite limit.
	 *
	 * @return The block chain rewrite limit.
	 */
	public int getBlockChainRewriteLimit() {
		return this.blockChainRewriteLimit;
	}

	/**
	 * Gets the estimated number of blocks per day.
	 *
	 * @return The estimated number of blocks per day.
	 */
	public int getEstimatedBlocksPerDay() {
		return 60 * 60 * 24 / this.blockGenerationTargetTime;
	}

	/**
	 * Gets the estimated number of blocks per month.
	 *
	 * @return The estimated number of blocks per month.
	 */
	public int getEstimatedBlocksPerMonth() {
		return this.getEstimatedBlocksPerDay() * 30;
	}

	/**
	 * Gets the estimated number of blocks per year.
	 *
	 * @return The estimated number of blocks per year.
	 */
	public int getEstimatedBlocksPerYear() {
		return this.getEstimatedBlocksPerDay() * 365;
	}

	/**
	 * Gets a value indicating whether or not the block chain supports the specified feature.
	 *
	 * @return true if the block chain supports the specified feature.
	 */
	public boolean isBlockChainFeatureSupported(final BlockChainFeature feature) {
		return Arrays.stream(this.blockChainFeatures).anyMatch(f -> f == feature);
	}

	/**
	 * Gets the default configuration.
	 *
	 * @return the default configuration.
	 */
	public static BlockChainConfiguration defaultConfiguration() {
		return new BlockChainConfiguration(
				BlockChainConstants.DEFAULT_TRANSACTIONS_LIMIT,
				BlockChainConstants.DEFAULT_MAX_TRANSACTIONS_PER_BLOCK,
				BlockChainConstants.DEFAULT_BLOCK_GENERATION_TARGET_TIME,
				BlockChainConstants.DEFAULT_REWRITE_LIMIT,
				BlockChainFeature.explode(BlockChainFeature.PROOF_OF_IMPORTANCE.value()));
	}
}
