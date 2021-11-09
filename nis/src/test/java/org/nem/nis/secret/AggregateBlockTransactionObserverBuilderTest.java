package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class AggregateBlockTransactionObserverBuilderTest {
	private static final BalanceAdjustmentNotification NOTIFICATION = new BalanceAdjustmentNotification(NotificationType.BalanceCredit,
			Utils.generateRandomAccount(), Amount.fromNem(12));
	private static final BlockNotificationContext NOTIFICATION_CONTEXT = NisUtils.createBlockNotificationContext(new BlockHeight(11),
			NotificationTrigger.Execute);

	// region add

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

	// endregion

	// region add multiple

	@Test
	public void canAddMultipleObservers() {
		// Arrange:
		final AddMultipleTestContext context = new AddMultipleTestContext();

		// Act:
		final BlockTransactionObserver aggregate = context.builder.build();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		Mockito.verify(context.observer1, Mockito.only()).notifyReceive(new BlockHeight(11), NOTIFICATION.getAccount(),
				NOTIFICATION.getAmount());
		Mockito.verify(context.observer2, Mockito.only()).notify(NOTIFICATION, NOTIFICATION_CONTEXT);
		Mockito.verify(context.observer3, Mockito.only()).notifyReceive(new BlockHeight(11), NOTIFICATION.getAccount(),
				NOTIFICATION.getAmount());
		Mockito.verify(context.observer4, Mockito.only()).notify(NOTIFICATION, NOTIFICATION_CONTEXT);
	}

	@Test
	public void buildChainsMultipleObserversInOrder() {
		// Arrange:
		final AddMultipleTestContext context = new AddMultipleTestContext();

		// Act:
		final BlockTransactionObserver aggregate = context.builder.build();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		MatcherAssert.assertThat(context.visitIds, IsEqual.equalTo(Arrays.asList(1, 2, 3, 4)));
	}

	@Test
	public void buildReverseChainsMultipleObserversInReverseOrder() {
		// Arrange:
		final AddMultipleTestContext context = new AddMultipleTestContext();

		// Act:
		final BlockTransactionObserver aggregate = context.builder.buildReverse();
		aggregate.notify(NOTIFICATION, NOTIFICATION_CONTEXT);

		// Assert:
		MatcherAssert.assertThat(context.visitIds, IsEqual.equalTo(Arrays.asList(4, 3, 2, 1)));
	}

	private static class AddMultipleTestContext {
		private final BlockTransferObserver observer1 = Mockito.mock(BlockTransferObserver.class);
		private final BlockTransactionObserver observer2 = Mockito.mock(BlockTransactionObserver.class);
		private final BlockTransferObserver observer3 = Mockito.mock(BlockTransferObserver.class);
		private final BlockTransactionObserver observer4 = Mockito.mock(BlockTransactionObserver.class);
		private final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		private final List<Integer> visitIds = new ArrayList<>();

		private AddMultipleTestContext() {
			this.builder.add(this.observer1);
			this.builder.add(this.observer2);
			this.builder.add(this.observer3);
			this.builder.add(this.observer4);

			Mockito.doAnswer(this.createAnswer(1)).when(this.observer1).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
			Mockito.doAnswer(this.createAnswer(2)).when(this.observer2).notify(Mockito.any(), Mockito.any());
			Mockito.doAnswer(this.createAnswer(3)).when(this.observer3).notifyReceive(Mockito.any(), Mockito.any(), Mockito.any());
			Mockito.doAnswer(this.createAnswer(4)).when(this.observer4).notify(Mockito.any(), Mockito.any());
		}

		private Answer<?> createAnswer(final int id) {
			return invocationOnMock -> {
				this.visitIds.add(id);
				return null;
			};
		}
	}

	// endregion

	// region getName

	@Test
	public void getNameReturnsCommaSeparatedListOfInnerObservers() {
		// Arrange:
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(createObserverWithName("alpha"));
		builder.add(createObserverWithName("zeta"));
		builder.add(createObserverWithName("gamma"));
		final BlockTransactionObserver observer = builder.build();

		// Act:
		final String name = observer.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("alpha,zeta,gamma"));
	}

	private static BlockTransactionObserver createObserverWithName(final String name) {
		final BlockTransactionObserver validator = Mockito.mock(BlockTransactionObserver.class);
		Mockito.when(validator.getName()).thenReturn(name);
		return validator;
	}

	// endregion
}
