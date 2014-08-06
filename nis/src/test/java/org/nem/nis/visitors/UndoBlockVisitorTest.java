package org.nem.nis.visitors;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;

public class UndoBlockVisitorTest {

	@Test
	public void visitorCallsUndoOnBlock() {
		// Arrange:
		final Block block = Mockito.mock(Block.class);
		final UndoBlockVisitor visitor = new UndoBlockVisitor(Mockito.mock(BlockTransferObserver.class));

		// Act:
		visitor.visit(null, block);

		// Assert:
		Mockito.verify(block, Mockito.times(1)).undo();
	}

	@Test
	public void observerIsNotifiedOfUndoTransfers() {
		// Arrange:
		final Block block = createUndoableBlock();
		final BlockTransferObserver mockObserver = Mockito.mock(BlockTransferObserver.class);
		final UndoBlockVisitor visitor = new UndoBlockVisitor(mockObserver);

		// Act:
		visitor.visit(null, block);

		// Assert:
		verifyCallCounts(mockObserver, 0, 0, 1, 2);
	}

	@Test
	public void observerIsUnregisteredByVisitor() {
		// Arrange:
		final Block block = createUndoableBlock();
		final BlockTransferObserver mockObserver = Mockito.mock(BlockTransferObserver.class);
		final UndoBlockVisitor visitor = new UndoBlockVisitor(mockObserver);

		// Act:
		visitor.visit(null, block);
		block.execute();

		// Assert:
		verifyCallCounts(mockObserver, 0, 0, 1, 2);
	}

	private static Block createUndoableBlock() {
		// Arrange:
		final Account account1 = createAccountWithBalance(Amount.fromNem(1000));
		final Account account2 = createAccountWithBalance(Amount.fromNem(1000));
		final Block block = NisUtils.createRandomBlockWithHeight(1);

		final MockTransaction transaction = new MockTransaction(account1);
		transaction.setFee(Amount.fromNem(100));
		transaction.setTransferAction(o -> o.notifyTransfer(account1, account2, Amount.fromNem(55)));

		block.addTransaction(transaction);

		// in order for the weighted balance to successfully undo the block, execute it first
		block.execute();
		return block;
	}

	private static Account createAccountWithBalance(final Amount amount) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(amount);
		account.getWeightedBalances().addReceive(BlockHeight.ONE, amount);
		return account;
	}

	private static void verifyCallCounts(
			final BlockTransferObserver observer,
			final int notifySendCounts,
			final int notifyReceiveCounts,
			final int notifySendUndoCounts,
			final int notifyReceiveUndoCounts) {
		Mockito.verify(observer, Mockito.times(notifySendCounts)).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(notifyReceiveCounts)).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(notifySendUndoCounts)).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(notifyReceiveUndoCounts)).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}
}
