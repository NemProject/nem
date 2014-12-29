package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

import java.util.*;

public class PruningObserverTest {
	private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY = 31 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY_OLD = 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long PRUNE_INTERVAL = 360;
	private static final long BETA_OUTLINK_PRUNING_FORK = BlockMarkerConstants.BETA_OUTLINK_PRUNING_FORK;
	private static final int RETENTION_HOURS = 42;

	//region no-op

	@Test
	public void noPruningIsTriggeredWhenNotificationTriggerIsNotExecute() {
		// Assert:
		assertNoPruning(432001, 1, NotificationTrigger.Undo, NotificationType.BlockHarvest);
	}

	@Test
	public void noPruningIsTriggeredWhenNotificationTypeIsNotHarvestReward() {
		// Assert:
		assertNoPruning(432001, 1, NotificationTrigger.Execute, NotificationType.BalanceCredit);
	}

	@Test
	public void noPruningIsTriggeredWhenBlockHeightModuloThreeHundredSixtyIsNotOne() {
		// Assert:
		for (int i = 1; i < 1000; ++i) {
			if (1 != (i % PRUNE_INTERVAL)) {
				assertNoPruning(i, 1, NotificationTrigger.Execute, NotificationType.BlockHarvest);
			}
		}
	}

	private static void assertNoPruning(
			final long notificationHeight,
			final int notificationTime,
			final NotificationTrigger notificationTrigger,
			final NotificationType notificationType) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Notification notification = createAdjustmentNotification(notificationType);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				new BlockHeight(notificationHeight),
				new TimeInstant(notificationTime),
				notificationTrigger);
		context.observer.notify(notification, notificationContext);

		// Assert:
		context.assertNoPruning();
	}

	//endregion

	//region block pruning trigger

	@Test
	public void blockBasedPruningIsTriggeredAtInitialBlockHeight() {
		// Assert:
		assertBlockBasedPruning(1, 1, 1);
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsNearWeightedBalanceBlockHistory() {
		// Assert:
		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY, 0, 0);
		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 1, 1, 1);

		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL, 0, 0);
		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 1, 361, 1);
		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 2, 0, 0);

		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL, 0, 0);
		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 1, 721, 1);
		assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 2, 0, 0);
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsNearOutlinkBlockHistory() {
		// Arrange:
		// TODO: Replace with new constant when launching.
		final long outlinkHistory = OUTLINK_BLOCK_HISTORY_OLD;
		final long historyDifference = OUTLINK_BLOCK_HISTORY_OLD - WEIGHTED_BALANCE_BLOCK_HISTORY;

		// Assert:
		assertBlockBasedPruning(outlinkHistory, 0, 0);
		assertBlockBasedPruning(outlinkHistory + 1, historyDifference + 1, 1);

		assertBlockBasedPruning(outlinkHistory + 360, 0, 0);
		assertBlockBasedPruning(outlinkHistory + 361, historyDifference + 361, 361);
		assertBlockBasedPruning(outlinkHistory + 362, 0, 0);

		assertBlockBasedPruning(outlinkHistory + 720, 0, 0);
		assertBlockBasedPruning(outlinkHistory + 721, historyDifference + 721, 721);
		assertBlockBasedPruning(outlinkHistory + 722, 0, 0);
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsMuchGreaterThanHistories() {
		// Assert:
		assertBlockBasedPruning(
				10 * OUTLINK_BLOCK_HISTORY + 1,
				10 * OUTLINK_BLOCK_HISTORY - WEIGHTED_BALANCE_BLOCK_HISTORY + 1,
				10 * OUTLINK_BLOCK_HISTORY - OUTLINK_BLOCK_HISTORY + 1);
	}

	@Test
	public void outlinkPruningUsesOutlinkBlockHistoryOldBeforeBetaOutlinkPruningFork() {
		// Assert:
		final long notificationHeight = (BETA_OUTLINK_PRUNING_FORK / 360) * 360 + 1;
		assertBlockBasedPruning(
				notificationHeight,
				notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY,
				notificationHeight - OUTLINK_BLOCK_HISTORY_OLD);
	}

	@Test
	public void outlinkPruningUsesOutlinkBlockHistoryAfterBetaOutlinkPruningFork() {
		// Assert:
		final long notificationHeight = (BETA_OUTLINK_PRUNING_FORK / 360 + 1) * 360 + 1;
		assertBlockBasedPruning(
				notificationHeight,
				notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY,
				notificationHeight - OUTLINK_BLOCK_HISTORY);
	}

	private static void assertBlockBasedPruning(
			final long notificationHeight,
			final long expectedWeightedBalancePruningHeight,
			final long expectedOutlinkPruningHeight) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Notification notification = createAdjustmentNotification(NotificationType.BlockHarvest);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				new BlockHeight(notificationHeight),
				TimeInstant.ZERO,
				NotificationTrigger.Execute);
		context.observer.notify(notification, notificationContext);

		// Assert:
		if (0 != expectedWeightedBalancePruningHeight) {
			context.assertWeightedBalancePruning(new BlockHeight(expectedWeightedBalancePruningHeight));
		} else {
			context.assertNoWeightedBalancePruning();
		}

		if (0 != expectedOutlinkPruningHeight) {
			context.assertOutlinkPruning(new BlockHeight(expectedOutlinkPruningHeight));
		} else {
			context.assertNoOutlinkPruning();
		}
	}

	//endregion

	//region time pruning trigger

	@Test
	public void timeBasedPruningIsTriggeredAtInitialTime() {
		// Assert:
		assertTimeBasedPruning(TimeInstant.ZERO);
	}

	@Test
	public void timeBasedPruningIsTriggeredAtAllTimes() {
		// Assert:
		final TimeInstant relativeTime1 = TimeInstant.ZERO.addHours(RETENTION_HOURS);
		assertTimeBasedPruning(relativeTime1.addSeconds(-1));
		assertTimeBasedPruning(relativeTime1);
		assertTimeBasedPruning(relativeTime1.addSeconds(1));

		final TimeInstant relativeTime2 = TimeInstant.ZERO.addHours(2 * RETENTION_HOURS);
		assertTimeBasedPruning(relativeTime2.addSeconds(-1));
		assertTimeBasedPruning(relativeTime2);
		assertTimeBasedPruning(relativeTime2.addSeconds(1));

		final TimeInstant relativeTime3 = TimeInstant.ZERO.addHours(3 * RETENTION_HOURS);
		assertTimeBasedPruning(relativeTime3.addSeconds(-1));
		assertTimeBasedPruning(relativeTime3);
		assertTimeBasedPruning(relativeTime3.addSeconds(1));
	}

	private static void assertTimeBasedPruning(final TimeInstant notificationTime) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Notification notification = createAdjustmentNotification(NotificationType.BlockHarvest);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				BlockHeight.ONE,
				notificationTime,
				NotificationTrigger.Execute);
		context.observer.notify(notification, notificationContext);

		// Assert:
		context.assertTransactionHashCachePruning(notificationTime);
	}

	//endregion

	private static Notification createAdjustmentNotification(final NotificationType type) {
		return new BalanceAdjustmentNotification(type, Utils.generateRandomAccount(), Amount.ZERO);
	}

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
		private final List<AccountState> accountStates = new ArrayList<>();
		private final BlockTransactionObserver observer = new PruningObserver(this.accountStateCache, this.transactionHashCache);

		private TestContext() {
			for (int i = 0; i < 3; ++i) {
				final AccountState accountState = Mockito.mock(AccountState.class);
				final AccountImportance accountImportance = Mockito.mock(AccountImportance.class);
				final WeightedBalances weightedBalances = Mockito.mock(WeightedBalances.class);

				Mockito.when(accountState.getImportanceInfo()).thenReturn(accountImportance);
				Mockito.when(accountState.getWeightedBalances()).thenReturn(weightedBalances);
				this.accountStates.add(accountState);
			}

			Mockito.when(this.accountStateCache.mutableContents()).thenReturn(new CacheContents<>(this.accountStates));
			Mockito.when(this.transactionHashCache.getRetentionTime()).thenReturn(RETENTION_HOURS);
		}

		private void assertNoPruning() {
			this.assertNoWeightedBalancePruning();
			this.assertNoOutlinkPruning();
			this.assertNoTransactionHashCachePruning();
		}

		private void assertNoWeightedBalancePruning() {
			for (final AccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getWeightedBalances(), Mockito.never()).prune(Mockito.any());
			}
		}

		private void assertNoOutlinkPruning() {
			for (final AccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getImportanceInfo(), Mockito.never()).prune(Mockito.any());
			}
		}

		private void assertNoTransactionHashCachePruning() {
			Mockito.verify(this.transactionHashCache, Mockito.never()).prune(Mockito.any());
		}

		private void assertWeightedBalancePruning(final BlockHeight height) {
			for (final AccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getWeightedBalances(), Mockito.only()).prune(height);
			}
		}

		private void assertOutlinkPruning(final BlockHeight height) {
			for (final AccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getImportanceInfo(), Mockito.only()).prune(height);
			}
		}

		private void assertTransactionHashCachePruning(final TimeInstant timeStamp) {
			Mockito.verify(this.transactionHashCache, Mockito.times(1)).prune(timeStamp);
		}
	}
}