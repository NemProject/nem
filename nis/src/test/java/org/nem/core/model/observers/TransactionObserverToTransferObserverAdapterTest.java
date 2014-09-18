package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class TransactionObserverToTransferObserverAdapterTest {

	@Test
	public void notifyTransferIsForwardedAsAccountAndBalanceTransferNotifications() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final TransferObserver adapter = new TransactionObserverToTransferObserverAdapter(observer);
		adapter.notifyTransfer(sender, recipient, amount);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());

		{
			final AccountNotification notification = (AccountNotification)notificationCaptor.getAllValues().get(0);
			Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.Account));
			Assert.assertThat(notification.getAccount(), IsEqual.equalTo(recipient));
		}
		{
			final BalanceTransferNotification notification = (BalanceTransferNotification)notificationCaptor.getAllValues().get(1);
			Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceTransfer));
			Assert.assertThat(notification.getSender(), IsEqual.equalTo(sender));
			Assert.assertThat(notification.getRecipient(), IsEqual.equalTo(recipient));
			Assert.assertThat(notification.getAmount(), IsEqual.equalTo(amount));
		}
	}

	@Test
	public void notifyCreditIsForwardedAsAccountAndBalanceCreditNotifications() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final TransferObserver adapter = new TransactionObserverToTransferObserverAdapter(observer);
		adapter.notifyCredit(account, amount);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());

		{
			final AccountNotification notification = (AccountNotification)notificationCaptor.getAllValues().get(0);
			Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.Account));
			Assert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
		}
		{
			final BalanceAdjustmentNotification notification = (BalanceAdjustmentNotification)notificationCaptor.getAllValues().get(1);
			Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceCredit));
			Assert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
			Assert.assertThat(notification.getAmount(), IsEqual.equalTo(amount));
		}
	}

	@Test
	public void notifyDebitIsForwardedAsBalanceDebitNotifications() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(444);

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final TransferObserver adapter = new TransactionObserverToTransferObserverAdapter(observer);
		adapter.notifyDebit(account, amount);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());

		final BalanceAdjustmentNotification notification = (BalanceAdjustmentNotification)notificationCaptor.getValue();
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceDebit));
		Assert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
		Assert.assertThat(notification.getAmount(), IsEqual.equalTo(amount));
	}
}