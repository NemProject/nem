package org.nem.nis.visitors;

import org.nem.core.model.Block;

/**
 * Visitor that visits blocks.
 */
public interface BlockVisitor {

	/**
	 * Visits a block.
	 *
	 * @param block The block.
	 */
	public void visit(final Block block);
}