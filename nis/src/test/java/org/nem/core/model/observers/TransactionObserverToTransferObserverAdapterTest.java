package org.nem.core.model.observers;

import org.junit.*;
import org.mockito.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class TransactionObserverToTransferObserverAdapterTest {

	@Test
	public void notifyTransferIsForwardedBalanceTransferNotifications() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);
		final BalanceTransferNotification notification = new BalanceTransferNotification(sender, recipient, amount);

		// Act:
		final TransactionObserverToTransferObserverAdapter observer = Mockito.mock(TransactionObserverToTransferObserverAdapter.class);
		observer.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notifyTransfer(sender, recipient, amount);
		Mockito.verify(observer, Mockito.never()).notifyCredit(Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyDebit(Mockito.any(), Mockito.any());
	}

	@Test
	public void notifyCreditIsForwardedBalanceCreditNotifications() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount);

		// Act:
		final TransactionObserverToTransferObserverAdapter observer = Mockito.mock(TransactionObserverToTransferObserverAdapter.class);
		observer.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.never()).notifyTransfer(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.only()).notifyCredit(account, amount);
		Mockito.verify(observer, Mockito.never()).notifyDebit(Mockito.any(), Mockito.any());

	}

	@Test
	public void notifyDebitIsForwardedBalanceDebitNotifications() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, amount);

		// Act:
		final TransactionObserverToTransferObserverAdapter observer = Mockito.mock(TransactionObserverToTransferObserverAdapter.class);
		observer.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.never()).notifyTransfer(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyCredit(Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.only()).notifyDebit(account, amount);
	}

	@Test
	public void nonBalanceNotificationInNotForwarded() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(NotificationType.ImportanceTransfer, account, amount);

		// Act:
		final TransactionObserverToTransferObserverAdapter observer = Mockito.mock(TransactionObserverToTransferObserverAdapter.class);
		observer.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.never()).notifyTransfer(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyCredit(Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyDebit(Mockito.any(), Mockito.any());
	}
}