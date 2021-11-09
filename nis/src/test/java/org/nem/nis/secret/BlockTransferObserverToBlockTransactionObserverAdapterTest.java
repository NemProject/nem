package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.NisUtils;

public class BlockTransferObserverToBlockTransactionObserverAdapterTest {

	@Test
	public void getNameDelegatesToInnerObserver() {
		// Arrange:
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);
		Mockito.when(observer.getName()).thenReturn("inner");

		// Act:
		final String name = adapter.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("inner"));
		Mockito.verify(observer, Mockito.only()).getName();
	}

	@Test
	public void balanceTransferExecuteNotificationIsForwardedToBalanceTransferObserver() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(new BalanceTransferNotification(account1, account2, amount),
				NisUtils.createBlockNotificationContext(height, NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(observer, Mockito.times(1)).notifySend(height, account1, amount);
		Mockito.verify(observer, Mockito.times(1)).notifyReceive(height, account2, amount);

		Mockito.verify(observer, Mockito.times(1)).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(1)).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void balanceTransferUndoNotificationIsForwardedToBalanceTransferObserver() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(new BalanceTransferNotification(account1, account2, amount),
				NisUtils.createBlockNotificationContext(height, NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(observer, Mockito.times(1)).notifyReceiveUndo(height, account1, amount);
		Mockito.verify(observer, Mockito.times(1)).notifySendUndo(height, account2, amount);

		Mockito.verify(observer, Mockito.never()).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(1)).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(1)).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void balanceCreditExecuteNotificationIsForwardedToBalanceTransferObserver() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount),
				NisUtils.createBlockNotificationContext(height, NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(observer, Mockito.times(1)).notifyReceive(height, account, amount);

		Mockito.verify(observer, Mockito.never()).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(1)).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void balanceCreditUndoNotificationIsForwardedToBalanceTransferObserver() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount),
				NisUtils.createBlockNotificationContext(height, NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(observer, Mockito.times(1)).notifySendUndo(height, account, amount);

		Mockito.verify(observer, Mockito.never()).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(1)).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void balanceDebitExecuteNotificationIsForwardedToBalanceTransferObserver() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, amount),
				NisUtils.createBlockNotificationContext(height, NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(observer, Mockito.times(1)).notifySend(height, account, amount);

		Mockito.verify(observer, Mockito.times(1)).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void balanceDebitUndoNotificationIsForwardedToBalanceTransferObserver() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, amount),
				NisUtils.createBlockNotificationContext(height, NotificationTrigger.Undo));

		// Assert:
		Mockito.verify(observer, Mockito.times(1)).notifyReceiveUndo(height, account, amount);

		Mockito.verify(observer, Mockito.never()).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.times(1)).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void otherNotificationIsNotForwardedToBalanceTransferObserver() {
		// Arrange:
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(new AccountNotification(Utils.generateRandomAccount()),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(observer, Mockito.never()).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}
}
