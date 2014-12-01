package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;

public class BlockTransactionObserverToTransactionObserverAdapterTest {

	@Test
	public void notificationsAreForwardedToWrappedBlockTransactionObserver() {
		// Arrange:
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		// TODO 20141201 J-B: fine now but you might want to consider adding a helper function for creating the context (e.g. NisUtils.createNotificationContext());
		final BlockNotificationContext context = new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123), NotificationTrigger.Execute);
		final TransactionObserver adapter = new BlockTransactionObserverToTransactionObserverAdapter(observer, context);
		final Notification notification = new Notification(NotificationType.BalanceCredit) {
		};

		// Act:
		adapter.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(notification, context);
	}
}