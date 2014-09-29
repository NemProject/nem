package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;

public class TransactionObserverToBlockTransferObserverAdapterTest {

	@Test
	public void notificationsAreForwardedToWrappedTransactionObserver() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final BlockTransactionObserver adapter = new TransactionObserverToBlockTransferObserverAdapter(observer);
		final Notification notification = new Notification(NotificationType.BalanceCredit) {
		};

		// Act:
		adapter.notify(notification, Mockito.mock(BlockNotificationContext.class));

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(notification);
	}
}