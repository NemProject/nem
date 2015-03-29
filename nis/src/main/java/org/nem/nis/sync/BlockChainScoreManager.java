package org.nem.nis.sync;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockChainScore;

/**
 * Interface for managing block chain scores.
 */
public interface BlockChainScoreManager {

	/**
	 * Returns the overall score for the chain.
	 *
	 * @return the score.
	 */
	BlockChainScore getScore();

	/**
	 * Updates the score of this chain by adding the score derived from the specified block and parent block.
	 *
	 * @param parentBlock The parent block.
	 * @param block The block.
	 */
	void updateScore(final Block parentBlock, final Block block);
}
