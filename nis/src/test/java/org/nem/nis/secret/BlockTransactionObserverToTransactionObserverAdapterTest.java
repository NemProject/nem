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
		// TODO 20141201 J-B: fine now but you might want to consider adding a helper function for creating the context (e.g. NisUtils.createNotificationContext());
		// TODO 20141204 BR -> J: done, though I am not sure why we need that.
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