package org.nem.core.model;

import org.nem.core.utils.MustBe;

import java.util.Arrays;

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
	 */
	public void setMaxTransactionsPerSyncAttempt(final int maxTransactionsPerSyncAttempt) {
		this.maxTransactionsPerSyncAttempt = maxTransactionsPerSyncAttempt;
	}

	/**
	 * Sets the maximum number of transactions allowed in a single block.
	 *
	 * @param maxTransactionsPerBlock The maximum number of transactions allowed in a single block.
	 */
	public void setMaxTransactionsPerBlock(final int maxTransactionsPerBlock) {
		this.maxTransactionsPerBlock = maxTransactionsPerBlock;
	}

	/**
	 * Sets the target time between two blocks in seconds.
	 *
	 * @param blockGenerationTargetTime The target time between two blocks in seconds.
	 */
	public void setBlockGenerationTargetTime(final int blockGenerationTargetTime) {
		this.blockGenerationTargetTime = blockGenerationTargetTime;
	}

	/**
	 * Sets the block chain rewrite limit.
	 *
	 * @param blockChainRewriteLimit The block chain rewrite limit.
	 */
	public void setBlockChainRewriteLimit(final int blockChainRewriteLimit) {
		this.blockChainRewriteLimit = blockChainRewriteLimit;
	}

	/**
	 * Sets the block chain features.
	 *
	 * @param blockChainFeatures The block chain features.
	 */
	public void setBlockChainFeatures(final BlockChainFeature[] blockChainFeatures) {
		this.blockChainFeatures = blockChainFeatures;
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
		public boolean isBlockChainFeatureSupported(final BlockChainFeature feature) {
			return Arrays.stream(this.blockChainFeatures).anyMatch(f -> f == feature);
		}
	}
}