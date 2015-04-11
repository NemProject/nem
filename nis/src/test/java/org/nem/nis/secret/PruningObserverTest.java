package org.nem.nis.secret;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

import java.util.*;

// TODO 20150411 we should probably restructure these tests so that we have pairs for each PRUNE_HISTORICAL_DATA value (true / false)
// > (using enclosed)
// TODO 20150411 BR -> J: hope that is what you had in mind ^^

@RunWith(Enclosed.class)
public class PruningObserverTest {

	private static final boolean PRUNE_HISTORICAL_DATA = true;

	private static abstract class PruningObserverTestBase {
		private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
		private static final long OUTLINK_BLOCK_HISTORY = 31 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
		private static final long PRUNE_INTERVAL = 360;
		private static final int RETENTION_HOURS = 42;

		protected abstract boolean pruneHistoricalData();

		//region no-op

		@Test
		public void noPruningIsTriggeredWhenNotificationTriggerIsNotExecute() {
			// Assert:
			assertNoPruning(432001, 1, NotificationTrigger.Undo, NotificationType.BlockHarvest, pruneHistoricalData());
		}

		@Test
		public void noPruningIsTriggeredWhenNotificationTypeIsNotHarvestReward() {
			// Assert:
			assertNoPruning(432001, 1, NotificationTrigger.Execute, NotificationType.BalanceCredit, pruneHistoricalData());
		}

		@Test
		public void noPruningIsTriggeredWhenBlockHeightModuloThreeHundredSixtyIsNotOne() {
			// Assert:
			for (int i = 1; i < 1000; ++i) {
				if (1 != (i % PRUNE_INTERVAL)) {
					assertNoPruning(i, 1, NotificationTrigger.Execute, NotificationType.BlockHarvest, pruneHistoricalData());
				}
			}
		}

		private static void assertNoPruning(
				final long notificationHeight,
				final int notificationTime,
				final NotificationTrigger notificationTrigger,
				final NotificationType notificationType,
				final boolean pruneHistoricalData) {
			// Arrange:
			final TestContext context = new TestContext(pruneHistoricalData);

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
			this.assertBlockBasedPruning(1, 1, 1, true);
		}

		@Test
		public void blockBasedPruningIsTriggeredWhenBlockHeightIsNearWeightedBalanceBlockHistory() {
			// Assert:
			this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY, 0, 0, false);
			assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 1, 1, 1, true);

			this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL, 0, 0, false);
			this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 1, 361, 1, true);
			this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 2, 0, 0, false);

			this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL, 0, 0, false);
			this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 1, 721, 1, true);
			this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 2, 0, 0, false);
		}

		@Test
		public void blockBasedPruningIsTriggeredWhenBlockHeightIsNearOutlinkBlockHistory() {
			// Arrange:
			final long outlinkHistory = OUTLINK_BLOCK_HISTORY;
			final long historyDifference = OUTLINK_BLOCK_HISTORY - WEIGHTED_BALANCE_BLOCK_HISTORY;

			// Assert:
			this.assertBlockBasedPruning(outlinkHistory, 0, 0, false);
			this.assertBlockBasedPruning(outlinkHistory + 1, historyDifference + 1, 1, true);

			this.assertBlockBasedPruning(outlinkHistory + 360, 0, 0, false);
			this.assertBlockBasedPruning(outlinkHistory + 361, historyDifference + 361, 361, true);
			this.assertBlockBasedPruning(outlinkHistory + 362, 0, 0, false);

			this.assertBlockBasedPruning(outlinkHistory + 720, 0, 0, false);
			this.assertBlockBasedPruning(outlinkHistory + 721, historyDifference + 721, 721, true);
			this.assertBlockBasedPruning(outlinkHistory + 722, 0, 0, false);
		}

		@Test
		public void blockBasedPruningIsTriggeredWhenBlockHeightIsMuchGreaterThanHistories() {
			// Assert:
			this.assertBlockBasedPruning(
					10 * OUTLINK_BLOCK_HISTORY + 1,
					10 * OUTLINK_BLOCK_HISTORY - WEIGHTED_BALANCE_BLOCK_HISTORY + 1,
					10 * OUTLINK_BLOCK_HISTORY - OUTLINK_BLOCK_HISTORY + 1,
					true);
		}

		@Test
		public void outlinkPruningUsesOutlinkBlockHistory() {
			// Assert:
			final long notificationHeight = 1234 * 360 + 1;
			this.assertBlockBasedPruning(
					notificationHeight,
					notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY,
					notificationHeight - OUTLINK_BLOCK_HISTORY,
					true);
		}

		private void assertBlockBasedPruning(
				final long notificationHeight,
				final long expectedWeightedBalancePruningHeight,
				final long expectedOutlinkPruningHeight,
				final boolean historicalImportancesPruning) {
			// Arrange:
			final TestContext context = new TestContext(pruneHistoricalData());

			// Act:
			final Notification notification = createAdjustmentNotification(NotificationType.BlockHarvest);
			final BlockNotificationContext notificationContext = new BlockNotificationContext(
					new BlockHeight(notificationHeight),
					TimeInstant.ZERO,
					NotificationTrigger.Execute);
			context.observer.notify(notification, notificationContext);

			// Assert:
			if (0 != expectedWeightedBalancePruningHeight && pruneHistoricalData()) {
				context.assertWeightedBalancePruning(new BlockHeight(expectedWeightedBalancePruningHeight));
			} else {
				context.assertNoWeightedBalancePruning();
			}

			if (0 != expectedOutlinkPruningHeight) {
				context.assertOutlinkPruning(new BlockHeight(expectedOutlinkPruningHeight));
			} else {
				context.assertNoOutlinkPruning();
			}

			if (historicalImportancesPruning && pruneHistoricalData()) {
				context.assertHistoricalImportancePruning();
			} else {
				context.assertNoHistoricalImportancePruning();
			}
		}

		//endregion

		//region time pruning trigger

		@Test
		public void timeBasedPruningIsTriggeredAtInitialTime() {
			// Assert:
			this.assertTimeBasedPruning(TimeInstant.ZERO);
		}

		@Test
		public void timeBasedPruningIsTriggeredAtAllTimes() {
			// Assert:
			final TimeInstant relativeTime1 = TimeInstant.ZERO.addHours(RETENTION_HOURS);
			this.assertTimeBasedPruning(relativeTime1.addSeconds(-1));
			this.assertTimeBasedPruning(relativeTime1);
			this.assertTimeBasedPruning(relativeTime1.addSeconds(1));

			final TimeInstant relativeTime2 = TimeInstant.ZERO.addHours(2 * RETENTION_HOURS);
			this.assertTimeBasedPruning(relativeTime2.addSeconds(-1));
			this.assertTimeBasedPruning(relativeTime2);
			this.assertTimeBasedPruning(relativeTime2.addSeconds(1));

			final TimeInstant relativeTime3 = TimeInstant.ZERO.addHours(3 * RETENTION_HOURS);
			this.assertTimeBasedPruning(relativeTime3.addSeconds(-1));
			this.assertTimeBasedPruning(relativeTime3);
			this.assertTimeBasedPruning(relativeTime3.addSeconds(1));
		}

		private void assertTimeBasedPruning(final TimeInstant notificationTime) {
			// Arrange:
			final TestContext context = new TestContext(pruneHistoricalData());

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
			private final BlockTransactionObserver observer;

			private TestContext(final boolean pruneHistoricalData) {
				this.observer = new PruningObserver(this.accountStateCache, this.transactionHashCache, pruneHistoricalData);

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

				Mockito.when(this.accountStateCache.mutableContents()).thenReturn(new CacheContents<>(this.accountStates));
				Mockito.when(this.transactionHashCache.getRetentionTime()).thenReturn(RETENTION_HOURS);
			}

			private void assertNoPruning() {
				this.assertNoWeightedBalancePruning();
				this.assertNoOutlinkPruning();
				this.assertNoHistoricalImportancePruning();
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

			private void assertNoHistoricalImportancePruning() {
				for (final AccountState accountState : this.accountStates) {
					Mockito.verify(accountState.getHistoricalImportances(), Mockito.never()).prune();
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

			private void assertHistoricalImportancePruning() {
				for (final AccountState accountState : this.accountStates) {
					Mockito.verify(accountState.getHistoricalImportances(), Mockito.only()).prune();
				}
			}

			private void assertTransactionHashCachePruning(final TimeInstant timeStamp) {
				Mockito.verify(this.transactionHashCache, Mockito.times(1)).prune(timeStamp);
			}
		}
	}

	public static class PruningObserverTestWithHistoricalDataPruning extends PruningObserverTestBase {

		@Override
		protected boolean pruneHistoricalData() {
			return PRUNE_HISTORICAL_DATA;
		}
	}

	public static class PruningObserverTestWithoutHistoricalDataPruning extends PruningObserverTestBase {

		@Override
		protected boolean pruneHistoricalData() {
			return !PRUNE_HISTORICAL_DATA;
		}
	}
}