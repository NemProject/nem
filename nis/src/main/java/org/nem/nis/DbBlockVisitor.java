package org.nem.nis;

import org.nem.nis.dbmodel.Block;

/**
 * Visitor that visits a block and its parent.
 */
interface DbBlockVisitor {

	/**
	 * Visits a block and its parent.
	 *
	 * @param dbParentBlock The parent block.
	 * @param dbBlock The block.
	 */
	public void visit(final Block dbParentBlock, final Block dbBlock);
}