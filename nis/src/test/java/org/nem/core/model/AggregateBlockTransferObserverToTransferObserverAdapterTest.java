package org.nem.core.model;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.test.Utils;

import java.util.*;

public class AggregateBlockTransferObserverToTransferObserverAdapterTest {

	@Test
	public void notifyTransferDelegatesToSubObserversForExecute() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final List<BlockTransferObserver> blockTransferObservers = createBlockTransferObservers();
		final TransferObserver aggregateObserver =
				new AggregateBlockTransferObserverToTransferObserverAdapter(blockTransferObservers, height, true);

		// Act:
		aggregateObserver.notifyTransfer(account1, account2, amount);

		// Assert:
		for (final BlockTransferObserver blockTransferObserver : blockTransferObservers) {
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifySend(height, account1, amount);
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifyReceive(height, account2, amount);
		}
	}

	@Test
	public void notifyTransferDelegatesToSubObserversForUndo() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final List<BlockTransferObserver> blockTransferObservers = createBlockTransferObservers();
		final TransferObserver aggregateObserver =
				new AggregateBlockTransferObserverToTransferObserverAdapter(blockTransferObservers, height, false);

		// Act:
		aggregateObserver.notifyTransfer(account1, account2, amount);

		// Assert:
		for (final BlockTransferObserver blockTransferObserver : blockTransferObservers) {
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifyReceiveUndo(height, account1, amount);
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifySendUndo(height, account2, amount);
		}
	}

	@Test
	public void notifyCreditDelegatesToSubObserversForExecute() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final List<BlockTransferObserver> blockTransferObservers = createBlockTransferObservers();
		final TransferObserver aggregateObserver =
				new AggregateBlockTransferObserverToTransferObserverAdapter(blockTransferObservers, height, true);

		// Act:
		aggregateObserver.notifyCredit(account, amount);

		// Assert:
		for (final BlockTransferObserver blockTransferObserver : blockTransferObservers)
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifyReceive(height, account, amount);
	}

	@Test
	public void notifyCreditDelegatesToSubObserversForUndo() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final List<BlockTransferObserver> blockTransferObservers = createBlockTransferObservers();
		final TransferObserver aggregateObserver =
				new AggregateBlockTransferObserverToTransferObserverAdapter(blockTransferObservers, height, false);

		// Act:
		aggregateObserver.notifyCredit(account, amount);

		// Assert:
		for (final BlockTransferObserver blockTransferObserver : blockTransferObservers)
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifySendUndo(height, account, amount);
	}

	@Test
	public void notifyDebitDelegatesToSubObserversForExecute() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final List<BlockTransferObserver> blockTransferObservers = createBlockTransferObservers();
		final TransferObserver aggregateObserver =
				new AggregateBlockTransferObserverToTransferObserverAdapter(blockTransferObservers, height, true);

		// Act:
		aggregateObserver.notifyDebit(account, amount);

		// Assert:
		for (final BlockTransferObserver blockTransferObserver : blockTransferObservers)
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifySend(height, account, amount);
	}

	@Test
	public void notifyDebitDelegatesToSubObserversForUndo() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final List<BlockTransferObserver> blockTransferObservers = createBlockTransferObservers();
		final TransferObserver aggregateObserver =
				new AggregateBlockTransferObserverToTransferObserverAdapter(blockTransferObservers, height, false);

		// Act:
		aggregateObserver.notifyDebit(account, amount);

		// Assert:
		for (final BlockTransferObserver blockTransferObserver : blockTransferObservers)
			Mockito.verify(blockTransferObserver, Mockito.times(1)).notifyReceiveUndo(height, account, amount);
	}

	private static List<BlockTransferObserver> createBlockTransferObservers() {
		return Arrays.asList(
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class),
				Mockito.mock(BlockTransferObserver.class));
	}
}