package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class TransferObserverToTransactionObserverAdapterTest {

	@Test
	public void getNameDelegatesToInnerObserver() {
		// Arrange:
		final TransferObserver observer = Mockito.mock(TransferObserver.class);
		final TransactionObserver adapter = new TransferObserverToTransactionObserverAdapter(observer);
		Mockito.when(observer.getName()).thenReturn("inner");

		// Act:
		final String name = adapter.getName();

		// Assert:
		Assert.assertThat(name, IsEqual.equalTo("inner"));
		Mockito.verify(observer, Mockito.only()).getName();
	}

	@Test
	public void notifyTransferIsForwardedBalanceTransferNotifications() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);
		final BalanceTransferNotification notification = new BalanceTransferNotification(sender, recipient, amount);

		// Act:
		final TransferObserver observer = Mockito.mock(TransferObserver.class);
		final TransactionObserver adapter = new TransferObserverToTransactionObserverAdapter(observer);
		adapter.notify(notification);

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
		final TransferObserver observer = Mockito.mock(TransferObserver.class);
		final TransactionObserver adapter = new TransferObserverToTransactionObserverAdapter(observer);
		adapter.notify(notification);

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
		final TransferObserver observer = Mockito.mock(TransferObserver.class);
		final TransactionObserver adapter = new TransferObserverToTransactionObserverAdapter(observer);
		adapter.notify(notification);

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
		final TransferObserver observer = Mockito.mock(TransferObserver.class);
		final TransactionObserver adapter = new TransferObserverToTransactionObserverAdapter(observer);
		adapter.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.never()).notifyTransfer(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyCredit(Mockito.any(), Mockito.any());
		Mockito.verify(observer, Mockito.never()).notifyDebit(Mockito.any(), Mockito.any());
	}
}