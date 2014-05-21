package org.nem.nis.visitors;

import org.nem.core.model.Block;
import org.nem.nis.BlockScorer;

/**
 * Block visitor that visits all blocks in a chain and calculates
 * a partial chain score.
 *
 * The first block is weighted twice as much as all other blocks:
 * 2*x_0 + x_1 + x_2 + ...
 */
public class PartialWeightedScoreVisitor implements BlockVisitor {

	/**
	 * The order of blocks passed to the visitor.
	 */
	public enum BlockOrder {
		/**
		 * The blocks are passed in forward order.
		 */
		Forward,

		/**
		 * The blocks are passed in reverse order.
		 */
		Reverse
	}

	private final BlockScorer blockScorer;
	private final BlockOrder order;
	private long lastScore;

	// visit should be called at most 1440 times, every score fits in 32-bits
	// so long will be enough to keep partial score
	private long partialScore;

	private int numVisitedBlocks;

	/**
	 * Creates a new visitor.
	 *
	 * @param scorer The scorer.
	 * @param order The order of visited blocks.
	 */
	public PartialWeightedScoreVisitor(final BlockScorer scorer, BlockOrder order) {
		this.blockScorer = scorer;
		this.order = order;
	}

	@Override
	public void visit(final Block parentBlock, final Block block) {

		this.lastScore = this.blockScorer.calculateBlockScore(parentBlock, block);

		if (0 == numVisitedBlocks++ && BlockOrder.Forward == this.order) {
			this.partialScore += this.lastScore;
		}

		this.partialScore += this.lastScore;
	}

	/**
	 * Gets the cumulative score.
	 *
	 * @return The cumulative score.
	 */
	public long getScore() {

		final long adjustment = BlockOrder.Reverse == this.order ? this.lastScore : 0;
		return this.partialScore + adjustment;
	}
}