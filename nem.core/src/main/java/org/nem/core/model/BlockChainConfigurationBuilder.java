package org.nem.core.model;

import org.nem.core.utils.MustBe;

/**
 * Builder used for creating block chain configuration.
 */
public class BlockChainConfigurationBuilder {
	private int maxTransactionsPerSyncAttempt = 10000;
	private int maxTransactionsPerBlock = 120;
	private int blockGenerationTargetTime = 60;
	private int blockChainRewriteLimit = 360;
	private BlockChainFeature[] blockChainFeatures = new BlockChainFeature[] { BlockChainFeature.PROOF_OF_IMPORTANCE };

	/**
	 * Sets the maximum number of transactions that a remote peer supplies in a chain part.
	 *
	 * @param maxTransactionsPerSyncAttempt The maximum number of transactions that a remote peer supplies in a chain part.
	 * @return This object for continuation.
	 */
	public BlockChainConfigurationBuilder setMaxTransactionsPerSyncAttempt(final int maxTransactionsPerSyncAttempt) {
		this.maxTransactionsPerSyncAttempt = maxTransactionsPerSyncAttempt;
		return this;
	}

	/**
	 * Sets the maximum number of transactions allowed in a single block.
	 *
	 * @param maxTransactionsPerBlock The maximum number of transactions allowed in a single block.
	 * @return This object for continuation.
	 */
	public BlockChainConfigurationBuilder setMaxTransactionsPerBlock(final int maxTransactionsPerBlock) {
		this.maxTransactionsPerBlock = maxTransactionsPerBlock;
		return this;
	}

	/**
	 * Sets the target time between two blocks in seconds.
	 *
	 * @param blockGenerationTargetTime The target time between two blocks in seconds.
	 * @return This object for continuation.
	 */
	public BlockChainConfigurationBuilder setBlockGenerationTargetTime(final int blockGenerationTargetTime) {
		this.blockGenerationTargetTime = blockGenerationTargetTime;
		return this;
	}

	/**
	 * Sets the block chain rewrite limit.
	 *
	 * @param blockChainRewriteLimit The block chain rewrite limit.
	 * @return This object for continuation.
	 */
	public BlockChainConfigurationBuilder setBlockChainRewriteLimit(final int blockChainRewriteLimit) {
		this.blockChainRewriteLimit = blockChainRewriteLimit;
		return this;
	}

	/**
	 * Sets the block chain features.
	 *
	 * @param blockChainFeatures The block chain features.
	 * @return This object for continuation.
	 */
	public BlockChainConfigurationBuilder setBlockChainFeatures(final BlockChainFeature[] blockChainFeatures) {
		this.blockChainFeatures = blockChainFeatures;
		return this;
	}

	/**
	 * Builds a new block chain configuration.
	 *
	 * @return The configuration.
	 */
	public BlockChainConfiguration build() {
		return new DefaultBlockChainConfiguration(this);
	}

	private static class DefaultBlockChainConfiguration implements BlockChainConfiguration {
		private final int maxTransactionsPerSyncAttempt;
		private final int maxTransactionsPerBlock;
		private final int blockGenerationTargetTime;
		private final int blockChainRewriteLimit;
		private final BlockChainFeature[] blockChainFeatures;

		public DefaultBlockChainConfiguration(final BlockChainConfigurationBuilder builder) {
			this.maxTransactionsPerSyncAttempt = builder.maxTransactionsPerSyncAttempt;
			this.maxTransactionsPerBlock = builder.maxTransactionsPerBlock;
			this.blockGenerationTargetTime = builder.blockGenerationTargetTime;
			this.blockChainRewriteLimit = builder.blockChainRewriteLimit;
			this.blockChainFeatures = builder.blockChainFeatures;

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

		@Override
		public int getMaxTransactionsPerSyncAttempt() {
			return this.maxTransactionsPerSyncAttempt;
		}

		@Override
		public int getMaxTransactionsPerBlock() {
			return this.maxTransactionsPerBlock;
		}

		@Override
		public int getBlockGenerationTargetTime() {
			return this.blockGenerationTargetTime;
		}

		@Override
		public int getBlockChainRewriteLimit() {
			return this.blockChainRewriteLimit;
		}

		@Override
		public BlockChainFeature[] getBlockChainFeatures() {
			return this.blockChainFeatures;
		}
	}
}