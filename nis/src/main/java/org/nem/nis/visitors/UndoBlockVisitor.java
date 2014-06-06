package org.nem.nis.visitors;

import org.nem.core.model.AccountsHeightObserver;
import org.nem.core.model.Block;
import org.nem.nis.AccountAnalyzer;

/**
 * Block visitor that undoes all blocks.
 */
public class UndoBlockVisitor implements BlockVisitor {

	final AccountAnalyzer accountAnalyzer;
	
	public UndoBlockVisitor(final AccountAnalyzer accountAnalyzer) {
		this.accountAnalyzer = accountAnalyzer;
	}
	
	@Override
	public void visit(final Block parentBlock, final Block block) {
		AccountsHeightObserver observer = new AccountsHeightObserver(this.accountAnalyzer);
		block.subscribe(observer);
		block.undo();
		block.unsubscribe(observer);
	}
}
