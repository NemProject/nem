package org.nem.nis.visitors;

import org.nem.core.model.Block;
import org.nem.nis.chain.BlockExecutor;
import org.nem.nis.secret.BlockTransactionObserver;

/**
 * Block visitor that undoes all blocks.
 */
public class UndoBlockVisitor implements BlockVisitor {
	private final BlockTransactionObserver observer;
	private final BlockExecutor executor;

	/**
	 * Creates a new undo block visitor.
	 *
	 * @param observer The observer.
	 * @param executor The executor
	 */
	public UndoBlockVisitor(final BlockTransactionObserver observer, final BlockExecutor executor) {
		this.observer = observer;
		this.executor = executor;
	}

	@Override
	public void visit(final Block parentBlock, final Block block) {
		this.executor.undo(block, this.observer);
	}
}
