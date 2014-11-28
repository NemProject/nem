package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class BlockTransferObserverToBlockTransactionObserverAdapterTest {

	@Test
	public void balanceTransferExecuteNotificationIsForwardedToBalanceTransferObserver() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(102);
		final BlockHeight height = new BlockHeight(19);
		final TimeInstant timeStamp = new TimeInstant(123);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(
				new BalanceTransferNotification(account1, account2, amount),
				new BlockNotificationContext(height, timeStamp, NotificationTrigger.Execute));

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
		final TimeInstant timeStamp = new TimeInstant(123);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(
				new BalanceTransferNotification(account1, account2, amount),
				new BlockNotificationContext(height, timeStamp, NotificationTrigger.Undo));

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
		final TimeInstant timeStamp = new TimeInstant(123);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount),
				new BlockNotificationContext(height, timeStamp, NotificationTrigger.Execute));

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
		final TimeInstant timeStamp = new TimeInstant(123);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount),
				new BlockNotificationContext(height, timeStamp, NotificationTrigger.Undo));

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
		final TimeInstant timeStamp = new TimeInstant(123);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, amount),
				new BlockNotificationContext(height, timeStamp, NotificationTrigger.Execute));

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
		final TimeInstant timeStamp = new TimeInstant(123);

		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver adapter = new BlockTransferObserverToBlockTransactionObserverAdapter(observer);

		// Act:
		adapter.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, amount),
				new BlockNotificationContext(height, timeStamp, NotificationTrigger.Undo));

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
		adapter.notify(
				new AccountNotification(Utils.generateRandomAccount()),
				new BlockNotificationContext(new BlockHeight(19), new TimeInstant(123), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(observer, Mockito.never()).notifySend(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifySendUndo(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyReceiveUndo(Mockito.any(), Mockito.any(), Mockito.any());
	}
}