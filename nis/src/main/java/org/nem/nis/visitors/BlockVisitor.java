package org.nem.nis.visitors;

import org.nem.core.model.Block;

/**
 * Visitor that visits blocks.
 */
public interface BlockVisitor {

	/**
	 * Visits a block.
	 *
	 * @param parentBlock The parent block, that is earlier in the chain.
	 * @param block The block.
	 */
	void visit(final Block parentBlock, final Block block);
}
