package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.nis.BlockScorer;
import org.nem.nis.cache.ReadOnlyAccountStateCache;

import java.math.BigInteger;
import java.util.*;

/**
 * A mock BlockScorer implementation.
 */
public class MockBlockScorer extends BlockScorer {

	private final Set<Block> zeroTargetBlocks = new HashSet<>();
	private final Map<BlockScoreKey, Long> blockScores = new HashMap<>();

	public MockBlockScorer() {
		super(Mockito.mock(ReadOnlyAccountStateCache.class));
	}

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
	 * @param parentBlock The parent block.
	 * @param block The block.
	 * @param score The score.
	 */
	public void setBlockScore(final Block parentBlock, final Block block, final long score) {
		this.blockScores.put(new BlockScoreKey(parentBlock, block), score);
	}

	@Override
	public BigInteger calculateHit(final Block block) {
		return BigInteger.ZERO;
	}

	@Override
	public BigInteger calculateTarget(final Block prevBlock, final Block block) {
		return this.zeroTargetBlocks.contains(block) ? BigInteger.ZERO : BigInteger.TEN;
	}

	@Override
	public long calculateBlockScore(final Block parentBlock, final Block block) {
		return this.blockScores.get(new BlockScoreKey(parentBlock, block));
	}

	private static class BlockScoreKey {
		private final Block parentBlock;
		private final Block block;

		public BlockScoreKey(final Block parentBlock, final Block block) {
			this.parentBlock = parentBlock;
			this.block = block;
		}

		@Override
		public int hashCode() {
			return this.parentBlock.hashCode() ^ this.block.hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof BlockScoreKey)) {
				return false;
			}

			final BlockScoreKey rhs = (BlockScoreKey) obj;
			return this.parentBlock.equals(rhs.parentBlock) && this.block.equals(rhs.block);
		}
	}
}
