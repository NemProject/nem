package org.nem.nis.test;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.nis.BlockScorer;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.sync.BlockChainScoreManager;

/**
 * A mock BlockChainScoreManager implementation.
 */
public class MockBlockChainScoreManager implements BlockChainScoreManager {
	private final ReadOnlyAccountStateCache accountStateCache;
	private BlockChainScore score = BlockChainScore.ZERO;

	public MockBlockChainScoreManager(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public BlockChainScore getScore() {
		return this.score;
	}

	@Override
	public void updateScore(final Block parentBlock, final Block block) {
		final BlockScorer scorer = new BlockScorer(this.accountStateCache);
		this.score = this.score.add(new BlockChainScore(scorer.calculateBlockScore(parentBlock, block)));
	}
}
