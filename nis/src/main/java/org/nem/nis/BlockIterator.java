package org.nem.nis;

import org.nem.core.model.Block;
import org.nem.core.model.BlockHeight;
import org.nem.nis.sync.BlockLookup;
import org.nem.nis.visitors.BlockVisitor;

import java.util.List;

/**
 * Static class that provides functions for iterating over a chain of blocks.
 */
public class BlockIterator {

	/**
	 * Unwinds the block chain until the desired height is reached and calls the visitor.
	 */
	public static void unwindUntil(
			final BlockLookup lookup,
			final BlockHeight desiredHeight,
			final BlockVisitor visitor) {

		Block currentBlock = lookup.getLastBlock();
		while (true) {
			BlockHeight currentHeight = currentBlock.getHeight();
			if (currentHeight.equals(desiredHeight))
				return;

			visitor.visit(currentBlock);
			currentHeight = currentHeight.prev();
			currentBlock = lookup.getBlockAt(currentHeight);
		}
	}

	/**
	 * Calls the visitor for all blocks.
	 */
	public static void all(final List<Block> blocks, final BlockVisitor visitor) {
		for (final Block block : blocks) {
			visitor.visit(block);
		}
	}
}
