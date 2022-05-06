package org.nem.nis;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.sync.BlockLookup;
import org.nem.nis.visitors.BlockVisitor;

import java.util.Collection;

/**
 * Static class that provides functions for iterating over a chain of blocks.
 */
public class BlockIterator {

	/**
	 * Unwinds the block chain until the desired height is reached and calls the visitor.
	 *
	 * @param lookup The block lookup.
	 * @param desiredHeight The height.
	 * @param visitor The visitor.
	 */
	public static void unwindUntil(final BlockLookup lookup, final BlockHeight desiredHeight, final BlockVisitor visitor) {

		Block currentBlock = lookup.getLastBlock();

		BlockHeight currentHeight = currentBlock.getHeight();
		if (currentHeight.equals(desiredHeight)) {
			return;
		}

		Block parentBlock = lookup.getBlockAt(currentHeight.prev());
		while (true) {
			visitor.visit(parentBlock, currentBlock);
			currentBlock = parentBlock;
			currentHeight = currentBlock.getHeight();

			if (currentHeight.equals(desiredHeight)) {
				return;
			}

			parentBlock = lookup.getBlockAt(currentHeight.prev());
		}
	}

	/**
	 * Calls the visitor for all blocks.
	 *
	 * @param parentBlock The parent block.
	 * @param blocks The collection of blocks.
	 * @param visitor The visitor.
	 */
	public static void all(Block parentBlock, final Collection<Block> blocks, final BlockVisitor visitor) {
		for (final Block block : blocks) {
			visitor.visit(parentBlock, block);
			parentBlock = block;
		}
	}
}
