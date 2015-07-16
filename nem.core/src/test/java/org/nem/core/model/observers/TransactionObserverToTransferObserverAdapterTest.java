package org.nem.core.model.observers;

import org.junit.Test;
import org.mockito.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

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
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), recipient);
		NotificationUtils.assertBalanceTransferNotification(notificationCaptor.getAllValues().get(1), sender, recipient, amount);
	}

	@Test
	public void notifyTransferForSmartTilesIsForwardedAsAccountAndSmartTileTransferNotifications() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final SmartTile smartTile = Utils.createSmartTile(3);

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final TransferObserver adapter = new TransactionObserverToTransferObserverAdapter(observer);
		adapter.notifyTransfer(sender, recipient, smartTile);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), recipient);
		NotificationUtils.assertSmartTileTransferNotification(notificationCaptor.getAllValues().get(1), sender, recipient, smartTile);
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
		NotificationUtils.assertAccountNotification(notificationCaptor.getAllValues().get(0), account);
		NotificationUtils.assertBalanceCreditNotification(notificationCaptor.getAllValues().get(1), account, amount);
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
		NotificationUtils.assertBalanceDebitNotification(notificationCaptor.getValue(), account, amount);
	}
}