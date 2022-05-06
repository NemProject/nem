package org.nem.nis.sync;

/**
 * Provides contextual information that informs block chain comparisons.
 */
public class ComparisonContext {
	private final int maxNumBlocksToAnalyze;
	private final int maxNumBlocksToRewrite;

	/**
	 * Creates a new comparison context.
	 *
	 * @param maxNumBlocksToAnalyze The maximum number of blocks to rewrite.
	 * @param maxNumBlocksToRewrite The maximum number of blocks to analyze.
	 */
	public ComparisonContext(final int maxNumBlocksToAnalyze, final int maxNumBlocksToRewrite) {
		this.maxNumBlocksToAnalyze = maxNumBlocksToAnalyze;
		this.maxNumBlocksToRewrite = maxNumBlocksToRewrite;
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
}
