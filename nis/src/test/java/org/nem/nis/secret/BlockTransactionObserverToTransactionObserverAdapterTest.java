package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;

public class BlockTransactionObserverToTransactionObserverAdapterTest {

	@Test
	public void notificationsAreForwardedToWrappedBlockTransactionObserver() {
		// Arrange:
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final BlockNotificationContext context = new BlockNotificationContext(new BlockHeight(11), NotificationTrigger.Execute);
		final TransactionObserver adapter = new BlockTransactionObserverToTransactionObserverAdapter(observer, context);
		final Notification notification = new Notification(NotificationType.BalanceCredit) {
		};

		// Act:
		adapter.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(notification, context);
	}
}