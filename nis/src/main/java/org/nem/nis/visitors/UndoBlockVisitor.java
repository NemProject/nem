package org.nem.nis.visitors;

import org.nem.core.model.Block;
import org.nem.core.model.BlockTransferObserver;

/**
 * Block visitor that undoes all blocks.
 */
public class UndoBlockVisitor implements BlockVisitor {

	final BlockTransferObserver observer;
	
	public UndoBlockVisitor(final BlockTransferObserver observer) {
		this.observer = observer;
	}
	
	@Override
	public void visit(final Block parentBlock, final Block block) {
		block.subscribe(observer);
		block.undo();
		block.unsubscribe(observer);
	}
}
