package org.nem.nis.secret.pruning;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.*;

import java.util.*;

public abstract class AbstractPruningObserverTest {
	protected static final long PRUNE_INTERVAL = 360;
	protected static final int RETENTION_HOURS = 42;

	// region abstract functions

	/**
	 * Creates the pruning block observer.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	protected abstract BlockTransactionObserver createObserver(final NisCache nisCache);

	/**
	 * Asserts that pruning occurred.
	 *
	 * @param nisCache The NIS cache
	 * @param state User defined state.
	 */
	protected abstract void assertPruning(final NisCache nisCache, final long state);

	/**
	 * Asserts that no pruning occurred.
	 *
	 * @param nisCache The NIS cache
	 */
	protected abstract void assertNoPruning(final NisCache nisCache);

	// endregion

	// region no-op

	@Test
	public void noPruningIsTriggeredWhenNotificationTriggerIsNotExecute() {
		// Assert:
		this.assertNoPruning(120 * PRUNE_INTERVAL + 1, 1, NotificationTrigger.Undo, NotificationType.BlockHarvest);
	}

	@Test
	public void noPruningIsTriggeredWhenNotificationTypeIsNotHarvestReward() {
		// Assert:
		this.assertNoPruning(120 * PRUNE_INTERVAL + 1, 1, NotificationTrigger.Execute, NotificationType.BalanceCredit);
	}

	@Test
	public void noPruningIsTriggeredWhenBlockHeightModuloThreeHundredSixtyIsNotOne() {
		// Assert:
		for (int i = 1; i < 1000; ++i) {
			if (1 != (i % PRUNE_INTERVAL)) {
				this.assertNoPruning(i, 1, NotificationTrigger.Execute, NotificationType.BlockHarvest);
			}
		}
	}

	private void assertNoPruning(final long notificationHeight, final int notificationTime, final NotificationTrigger notificationTrigger,
			final NotificationType notificationType) {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = this.createObserver(context.nisCache);

		// Act:
		final Notification notification = createAdjustmentNotification(notificationType);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(new BlockHeight(notificationHeight),
				new TimeInstant(notificationTime), notificationTrigger);
		observer.notify(notification, notificationContext);

		// Assert:
		this.assertNoPruning(context.nisCache);
	}

	// endregion

	protected void assertBlockBasedPruning(final long notificationHeight, final long state) {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = this.createObserver(context.nisCache);

		// Act:
		final Notification notification = createAdjustmentNotification(NotificationType.BlockHarvest);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(new BlockHeight(notificationHeight),
				TimeInstant.ZERO, NotificationTrigger.Execute);
		observer.notify(notification, notificationContext);

		// Assert:
		this.assertPruning(context.nisCache, state);
	}

	protected void assertTimeBasedPruning(final TimeInstant notificationTime, final long state) {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockTransactionObserver observer = this.createObserver(context.nisCache);

		// Act:
		final Notification notification = createAdjustmentNotification(NotificationType.BlockHarvest);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(BlockHeight.ONE, notificationTime,
				NotificationTrigger.Execute);
		observer.notify(notification, notificationContext);

		// Assert:
		this.assertPruning(context.nisCache, state);
	}

	private static Notification createAdjustmentNotification(final NotificationType type) {
		return new BalanceAdjustmentNotification(type, Utils.generateRandomAccount(), Amount.ZERO);
	}

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
		private final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
		private final NisCache nisCache = Mockito.mock(NisCache.class);
		private final List<AccountState> accountStates = new ArrayList<>();

		private TestContext() {
			for (int i = 0; i < 3; ++i) {
				final AccountState accountState = Mockito.mock(AccountState.class);
				final AccountImportance accountImportance = Mockito.mock(AccountImportance.class);
				final HistoricalImportances historicalImportances = Mockito.mock(HistoricalImportances.class);
				final WeightedBalances weightedBalances = Mockito.mock(WeightedBalances.class);

				Mockito.when(accountState.getImportanceInfo()).thenReturn(accountImportance);
				Mockito.when(accountState.getWeightedBalances()).thenReturn(weightedBalances);
				Mockito.when(accountState.getHistoricalImportances()).thenReturn(historicalImportances);
				this.accountStates.add(accountState);
			}

			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);
			Mockito.when(this.nisCache.getTransactionHashCache()).thenReturn(this.transactionHashCache);
			Mockito.when(this.nisCache.getNamespaceCache()).thenReturn(this.namespaceCache);
			Mockito.when(this.accountStateCache.mutableContents()).thenReturn(new CacheContents<>(this.accountStates));
			Mockito.when(this.transactionHashCache.getRetentionTime()).thenReturn(RETENTION_HOURS);
		}
	}
}
