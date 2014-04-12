package org.nem.nis.sync;

import org.nem.nis.BlockScorer;

/**
 * Provides contextual information that informs block chain comparisons.
 */
public class ComparisonContext {

	private final int maxNumBlocksToAnalyze;
	private final int maxNumBlocksToRewrite;
	private final BlockScorer scorer;

	/**
	 * Creates a new comparison context.
	 *
	 * @param maxNumBlocksToAnalyze The maximum number of blocks to rewrite.
	 * @param maxNumBlocksToRewrite The maximum number of blocks to analyze.
	 * @param scorer The block scorer.
	 */
	public ComparisonContext(int maxNumBlocksToAnalyze, int maxNumBlocksToRewrite, final BlockScorer scorer) {
		this.maxNumBlocksToAnalyze = maxNumBlocksToAnalyze;
		this.maxNumBlocksToRewrite = maxNumBlocksToRewrite;
		this.scorer = scorer;
	}

	/**
	 * Gets the maximum number of blocks to analyze.
	 *
	 * @return The maximum number of blocks to analyze.
	 */
	public int getMaxNumBlocksToAnalyze() {
		return this.maxNumBlocksToAnalyze;
	}

	/**
	 * Gets the maximum number of blocks to rewrite.
	 *
	 * @return The maximum number of blocks to rewrite.
	 */
	public int getMaxNumBlocksToRewrite() {
		return this.maxNumBlocksToRewrite;
	}

	/**
	 * Gets the block scorer to use.
	 *
	 * @return The block scorer to use.
	 */
	public BlockScorer getBlockScorer() {
		return this.scorer;
	}
}
