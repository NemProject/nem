package org.nem.nis.visitors;

import org.nem.core.serialization.AccountLookup;
import org.nem.nis.BlockScorer;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.visitors.DbBlockVisitor;

/**
 * DB block visitor that visits all blocks in reverse order and calculates
 * a chain score.
 */
class PartialWeightedScoreVisitor implements DbBlockVisitor {
	private final BlockScorer blockScorer;
	private AccountLookup accountLookup;
	private long lastScore;
	private long partialScore;

	/**
	 * Creates a new visitor.
	 *
	 * @param scorer The scorer.
	 * @param accountLookup The account lookup.
	 */
	public PartialWeightedScoreVisitor(final BlockScorer scorer, boolean areBlocksVisitedInOrder) {
		this.blockScorer = scorer;
		this.accountLookup = accountLookup;
		this.lastScore = 0L;
		this.partialScore = 0L;
	}

	@Override
	public void visit(final Block dbParentBlock, final Block dbBlock) {
		// visit should be called at most 1440 times, every score fits in 32-bits
		// so long will be enough to keep partial score
//		this.lastScore = this.blockScorer.calculateBlockScore(dbParentBlock, dbBlock);
		this.partialScore += this.lastScore;
	}

	/**
	 * Gets the cumulative score.
	 *
	 * @return The cumulative score.
	 */
	public long getScore() {
		// equal to 2*x_0 + x_1 + x_2 + ...
		return this.partialScore + this.lastScore;
	}
}