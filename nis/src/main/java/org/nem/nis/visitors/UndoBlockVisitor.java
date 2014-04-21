package org.nem.nis.visitors;

import org.nem.core.model.Block;

/**
 * Block visitor that undoes all blocks.
 */
public class UndoBlockVisitor implements BlockVisitor {

	@Override
	public void visit(final Block parentBlock, final Block block) {
		block.undo();
	}
}
