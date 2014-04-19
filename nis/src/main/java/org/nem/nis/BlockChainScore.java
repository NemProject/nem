package org.nem.nis;

import org.nem.core.model.Block;

import java.util.Collection;

public class BlockChainScore {
	final BlockScorer scorer;

	public BlockChainScore(BlockScorer scorer) {
		this.scorer = scorer;
	}


	/**
	 * Computes partial score given blocks and parentBlock.
	 * TODO: add tests for it
	 *
	 * @param parentBlock The parent block.
	 * @param blocks The block chain.
	 *
	 * @return "partial score" of blocks.
	 */
	long computePartialScore(Block parentBlock, final Collection<Block> blocks) {
		long peersScore = 0L;

		// used to distinguish first element, to calculate:
		// 2*x_0 + x_1 + x_2 + ...
		boolean isFirst = true;
		for (final Block block : blocks) {
			long score = scorer.calculateBlockScore(parentBlock, block);

			block.getSigner().incrementForagedBlocks();

			peersScore += score;

			if (isFirst) {
				peersScore += score;
				isFirst = false;
			}

			parentBlock = block;
		}

		return peersScore;
	}
}
