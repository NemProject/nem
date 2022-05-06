package org.nem.nis.harvesting;

import org.nem.core.model.Block;

/**
 * Represents a generated block.
 */
public class GeneratedBlock {
	private final Block block;
	private final long score;

	/**
	 * Creates a new generated block.
	 *
	 * @param block The block.
	 * @param score The block score.
	 */
	public GeneratedBlock(final Block block, final long score) {
		this.block = block;
		this.score = score;
	}

	/**
	 * Gets the block.
	 *
	 * @return The block.
	 */
	public Block getBlock() {
		return this.block;
	}

	/**
	 * Gets the block score
	 *
	 * @return The block score.
	 */
	public long getScore() {
		return this.score;
	}
}
