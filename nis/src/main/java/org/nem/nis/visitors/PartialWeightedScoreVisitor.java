package org.nem.nis.visitors;

import org.nem.core.model.Block;
import org.nem.core.model.BlockChainScore;
import org.nem.nis.BlockScorer;

/**
 * Block visitor that visits all blocks in a chain and calculates
 * a partial chain score.
 *
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

	// visit should be called at most 1440 times, every score fits in 32-bits
	// so long will be enough to keep partial score
	private long partialScore;

	/**
	 * Creates a new visitor.
	 *
	 * @param scorer The scorer.
	 * @param order The order of visited blocks.
	 */
	public PartialWeightedScoreVisitor(final BlockScorer scorer) {
		this.blockScorer = scorer;
	}

	@Override
	public void visit(final Block parentBlock, final Block block) {

		this.partialScore += this.blockScorer.calculateBlockScore(parentBlock, block);
	}

	/**
	 * Gets the cumulative score.
	 *
	 * @return The cumulative score.
	 */
	public BlockChainScore getScore() {
		return new BlockChainScore(this.partialScore);
	}
}