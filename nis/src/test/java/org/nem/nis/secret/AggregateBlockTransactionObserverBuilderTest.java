package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class AggregateBlockTransactionObserverBuilderTest {
	private static final BalanceAdjustmentNotification NOTIFICATION = new BalanceAdjustmentNotification(
			NotificationType.BalanceCredit,
			Utils.generateRandomAccount(),
			Amount.fromNem(12));
	private static final BlockNotificationContext NOTIFICATION_CONTEXT = new BlockNotificationContext(
			new BlockHeight(11),
			NotificationTrigger.Execute);

	@Test
	public void canAddBlockTransactionObserver() {
		// Arrange:
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final BlockTransactionObserver aggregate = builder.build();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(NOTIFICATION, NOTIFICATION_CONTEXT);
	}

	@Test
	public void canAddTransactionObserver() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final BlockTransactionObserver aggregate = builder.build();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(NOTIFICATION);
	}

	@Test
	public void canAddBlockTransferObserver() {
		// Arrange:
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final BlockTransactionObserver aggregate = builder.build();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notifyReceive(new BlockHeight(11), NOTIFICATION.getAccount(), NOTIFICATION.getAmount());
	}

	@Test
	public void canAddTransferObserver() {
		// Arrange:
		final TransferObserver observer = Mockito.mock(TransferObserver.class);
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final BlockTransactionObserver aggregate = builder.build();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notifyCredit(NOTIFICATION.getAccount(), NOTIFICATION.getAmount());
	}

	@Test
	public void canAddMultipleObservers() {
		// Arrange:
		final BlockTransferObserver observer1 = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver observer2 = Mockito.mock(BlockTransactionObserver.class);
		final BlockTransferObserver observer3 = Mockito.mock(BlockTransferObserver.class);
		final BlockTransactionObserver observer4 = Mockito.mock(BlockTransactionObserver.class);
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();

		// Act:
		builder.add(observer1);
		builder.add(observer2);
		builder.add(observer3);
		builder.add(observer4);
		final BlockTransactionObserver aggregate = builder.build();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		Mockito.verify(observer1, Mockito.only()).notifyReceive(new BlockHeight(11), NOTIFICATION.getAccount(), NOTIFICATION.getAmount());
		Mockito.verify(observer2, Mockito.only()).notify(NOTIFICATION, NOTIFICATION_CONTEXT);
		Mockito.verify(observer3, Mockito.only()).notifyReceive(new BlockHeight(11), NOTIFICATION.getAccount(), NOTIFICATION.getAmount());
		Mockito.verify(observer4, Mockito.only()).notify(NOTIFICATION, NOTIFICATION_CONTEXT);
	}
}