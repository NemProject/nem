package org.nem.core.model;

import org.nem.core.utils.MustBe;

/**
 * Class that encapsulates block chain configuration data.
 */
public class BlockChainConfiguration {
	private final int maxTransactionsPerSyncAttempt;
	private final int maxTransactionsPerBlock;
	private final int blockGenerationTargetTime;
	private final int blockChainRewriteLimit;

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
		this.maxTransactionsPerSyncAttempt = maxTransactionsPerSyncAttempt;
		this.maxTransactionsPerBlock = maxTransactionsPerBlock;
		this.blockGenerationTargetTime = blockGenerationTargetTime;
		this.blockChainRewriteLimit = blockChainRewriteLimit;
		MustBe.inRange(
				this.maxTransactionsPerSyncAttempt,
				"max transactions per sync attempt",
				this.maxTransactionsPerBlock,
				this.maxTransactionsPerBlock * this.blockChainRewriteLimit);
		MustBe.inRange(this.maxTransactionsPerBlock, "max transactions per block", 1, 10_000);
		MustBe.inRange(this.blockGenerationTargetTime, "block generation target time", 10, 86_400);
		MustBe.inRange(this.blockChainRewriteLimit, "block chain rewrite limit", 10, this.getEstimatedBlocksPerDay());
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
	 * Gets the maximum number of transactions allowed in a single block.
	 *
	 * @return The maximum number of transactions.
	 */
	public int getMaxTransactionsPerBlock() {
		return this.maxTransactionsPerBlock;
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
}
