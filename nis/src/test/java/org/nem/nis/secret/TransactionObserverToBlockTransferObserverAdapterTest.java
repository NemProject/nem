package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;

public class TransactionObserverToBlockTransferObserverAdapterTest {

	@Test
	public void getNameDelegatesToInnerObserver() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final BlockTransactionObserver adapter = new TransactionObserverToBlockTransferObserverAdapter(observer);
		Mockito.when(observer.getName()).thenReturn("inner");

		// Act:
		final String name = adapter.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("inner"));
		Mockito.verify(observer, Mockito.only()).getName();
	}

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
