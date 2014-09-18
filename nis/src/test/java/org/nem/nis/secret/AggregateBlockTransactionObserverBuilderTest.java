package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class AggregateBlockTransactionObserverBuilderTest {

	@Test
	public void canAddTransactionObserver() {
		// Arrange:
		final Notification notification = new BalanceAdjustmentNotification(
				NotificationType.BalanceCredit,
				Utils.generateRandomAccount(),
				Amount.fromNem(12));
		final BlockNotificationContext context = new BlockNotificationContext(new BlockHeight(11), NotificationTrigger.Execute);
		final BlockTransactionObserver observer = Mockito.mock(BlockTransactionObserver.class);
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final BlockTransactionObserver aggregate = builder.build();
		aggregate.notify(notification, context);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(notification, context);
	}

	@Test
	public void canAddTransferObserver() {
		// Arrange:
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(
				NotificationType.BalanceCredit,
				Utils.generateRandomAccount(),
				Amount.fromNem(12));
		final BlockNotificationContext context = new BlockNotificationContext(new BlockHeight(11), NotificationTrigger.Execute);
		final BlockTransferObserver observer = Mockito.mock(BlockTransferObserver.class);
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final BlockTransactionObserver aggregate = builder.build();
		aggregate.notify(notification, context);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notifyReceive(new BlockHeight(11), notification.getAccount(), notification.getAmount());
	}

	@Test
	public void canAddMultipleObservers() {
		// Arrange:
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(
				NotificationType.BalanceCredit,
				Utils.generateRandomAccount(),
				Amount.fromNem(12));
		final BlockNotificationContext context = new BlockNotificationContext(new BlockHeight(11), NotificationTrigger.Execute);
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
		aggregate.notify(notification, context);

		// Assert:
		Mockito.verify(observer1, Mockito.only()).notifyReceive(new BlockHeight(11), notification.getAccount(), notification.getAmount());
		Mockito.verify(observer2, Mockito.only()).notify(notification, context);
		Mockito.verify(observer3, Mockito.only()).notifyReceive(new BlockHeight(11), notification.getAccount(), notification.getAmount());
		Mockito.verify(observer4, Mockito.only()).notify(notification, context);
	}
}