package org.nem.nis.visitors;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.nis.chain.BlockExecutor;
import org.nem.nis.secret.BlockTransactionObserver;

public class UndoBlockVisitorTest {

	@Test
	public void visitorCallsUndoOnBlockExecutor() {
		// Arrange:
		final Block block = Mockito.mock(Block.class);
		final TestContext context = new TestContext();

		// Act:
		context.visitor.visit(null, block);

		// Assert:
		Mockito.verify(context.executor, Mockito.times(1)).undo(block, context.observer);
	}

	private static class TestContext {
		private final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		private final BlockExecutor executor = Mockito.mock(BlockExecutor.class);
		private final UndoBlockVisitor visitor = new UndoBlockVisitor(this.observer, this.executor);
	}
}
