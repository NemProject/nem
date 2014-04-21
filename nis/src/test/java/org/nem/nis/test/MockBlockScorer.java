package org.nem.nis.test;

import org.nem.core.model.Block;
import org.nem.nis.BlockScorer;

import java.math.BigInteger;
import java.util.*;

/**
 * A mock BlockScorer implementation.
 */
public class MockBlockScorer extends BlockScorer {

	private Set<Block> zeroTargetBlocks = new HashSet<>();
	private Map<Block, Long> blockScores = new HashMap<>();

	/**
	 * Configures the scorer to return a ZERO target for the specified block.
	 *
	 * @param block The block.
	 */
	public void setZeroTargetBlock(final Block block) {
		this.zeroTargetBlocks.add(block);
	}

	/**
	 * Configures the scorer to return the specified score for the specified block.
	 *
	 * @param block The block.
	 * @param score The score.
	 */
	public void setBlockScore(final Block block, final long score) {
		this.blockScores.put(block, score);
	}

	@Override
	public BigInteger calculateHit(final Block block) {
		return BigInteger.ZERO;
	}

	@Override
	public BigInteger calculateTarget(final Block prevBlock, final Block block) {
		return zeroTargetBlocks.contains(block) ? BigInteger.ZERO : BigInteger.TEN;
	}

	@Override
	public long calculateBlockScore(final Block block) {
		return blockScores.get(block);
	}
}