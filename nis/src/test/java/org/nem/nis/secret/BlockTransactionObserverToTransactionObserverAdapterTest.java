package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.nis.test.NisUtils;

public class BlockTransactionObserverToTransactionObserverAdapterTest {

	@Test
	public void notificationsAreForwardedToWrappedBlockTransactionObserver() {
		// Arrange:
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final BlockNotificationContext context = NisUtils.createBlockNotificationContext();
		final TransactionObserver adapter = new BlockTransactionObserverToTransactionObserverAdapter(observer, context);
		final Notification notification = new Notification(NotificationType.BalanceCredit) {
		};

		// Act:
		adapter.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(notification, context);
	}
}
