package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class AggregateTransactionObserverBuilderTest {

	@Test
	public void canAddTransactionObserver() {
		// Arrange:
		final Notification notification = new BalanceAdjustmentNotification(
				NotificationType.BalanceCredit,
				Utils.generateRandomAccount(),
				Amount.fromNem(12));
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final AggregateTransactionObserverBuilder builder = new AggregateTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final TransactionObserver aggregate = builder.build();
		aggregate.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notify(notification);
	}

	@Test
	public void canAddTransferObserver() {
		// Arrange:
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(
				NotificationType.BalanceCredit,
				Utils.generateRandomAccount(),
				Amount.fromNem(12));
		final TransferObserver observer = Mockito.mock(TransferObserver.class);
		final AggregateTransactionObserverBuilder builder = new AggregateTransactionObserverBuilder();

		// Act:
		builder.add(observer);
		final TransactionObserver aggregate = builder.build();
		aggregate.notify(notification);

		// Assert:
		Mockito.verify(observer, Mockito.only()).notifyCredit(notification.getAccount(), notification.getAmount());
	}

	@Test
	public void canAddMultipleObservers() {
		// Arrange:
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(
				NotificationType.BalanceCredit,
				Utils.generateRandomAccount(),
				Amount.fromNem(12));
		final TransferObserver observer1 = Mockito.mock(TransferObserver.class);
		final TransactionObserver observer2 = Mockito.mock(TransactionObserver.class);
		final TransferObserver observer3 = Mockito.mock(TransferObserver.class);
		final TransactionObserver observer4 = Mockito.mock(TransactionObserver.class);
		final AggregateTransactionObserverBuilder builder = new AggregateTransactionObserverBuilder();

		// Act:
		builder.add(observer1);
		builder.add(observer2);
		builder.add(observer3);
		builder.add(observer4);
		final TransactionObserver aggregate = builder.build();
		aggregate.notify(notification);

		// Assert:
		Mockito.verify(observer1, Mockito.only()).notifyCredit(notification.getAccount(), notification.getAmount());
		Mockito.verify(observer2, Mockito.only()).notify(notification);
		Mockito.verify(observer3, Mockito.only()).notifyCredit(notification.getAccount(), notification.getAmount());
		Mockito.verify(observer4, Mockito.only()).notify(notification);
	}
}