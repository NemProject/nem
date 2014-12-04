package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.HashCache;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.poi.*;

import java.util.*;

public class PruningObserverTest {
	private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY = 31 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY_OLD = 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long PRUNE_INTERVAL = 360;
	private static final long BETA_OUTLINK_PRUNING_FORK = BlockMarkerConstants.BETA_OUTLINK_PRUNING_FORK;
	private static final int TRANSACTION_HASH_CACHE_HISTORY = 129600;

	//region no-op

	@Test
	public void pruneIsNotCalledWhenNotificationTriggerIsNotExecute() {
		// Assert:
		assertNoPruning(432001, 1, NotificationTrigger.Undo, NotificationType.HarvestReward);
	}

	@Test
	public void pruneIsNotCalledWhenNotificationTypeIsNotHarvestReward() {
		// Assert:
		assertNoPruning(432001, 1, NotificationTrigger.Execute, NotificationType.BalanceCredit);
	}

	@Test
	public void pruneIsNotCalledIfBlockHeightModuloThreeHundredSixtyIsNotOne() {
		// Assert:
		for (int i = 1; i < 1000; ++i) {
			if (1 != (i % PRUNE_INTERVAL)) {
				assertNoPruning(i, 1, NotificationTrigger.Execute, NotificationType.HarvestReward);
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

	//region pruning trigger

	@Test
	public void pruneIsCalledAtInitialBlockHeight() {
		// Assert:
		assertPruning(1, 0, 1, 1, 0);
	}

	@Test
	public void pruneIsCalledWhenBlockHeightIsNearWeightedBalanceBlockHistory() {
		// Assert:
		// TODO 20141201 J-B: why 1M? we should add a test where the transaction pruning value changes too
		// TODO 20141204 BR -> J: 1M is an arbitrary value. Block generation is a random process, there will not be a fixed time at a fixed height.
		// TODO                   Not sure what kind of additional you had in mind.
		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY, 1_000_000, 0, 0, -1);
		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 1, 1_000_000, 1, 1, 1_000_000 - TRANSACTION_HASH_CACHE_HISTORY);

		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL, 1_000_000, 0, 0, -1);
		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 1, 1_000_000, 361, 1, 1_000_000 - TRANSACTION_HASH_CACHE_HISTORY);
		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 2, 1_000_000, 0, 0, -1);

		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL, 1_000_000, 0, 0, -1);
		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 1, 1_000_000, 721, 1, 1_000_000 - TRANSACTION_HASH_CACHE_HISTORY);
		assertPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 2, 1_000_000, 0, 0, -1);
	}

	@Test
	public void pruneIsCalledWhenBlockHeightIsNearOutlinkBlockHistory() {
		// Arrange:
		// TODO: Replace with new constant when launching.
		final long outlinkHistory = OUTLINK_BLOCK_HISTORY_OLD;
		final long historyDifference = OUTLINK_BLOCK_HISTORY_OLD - WEIGHTED_BALANCE_BLOCK_HISTORY;

		// Assert:
		assertPruning(outlinkHistory, 1_000_000, 0, 0, -1);
		assertPruning(outlinkHistory + 1, 1_000_000, historyDifference + 1, 1, 1_000_000 - TRANSACTION_HASH_CACHE_HISTORY);

		assertPruning(outlinkHistory + 360, 1_000_000, 0, 0, -1);
		assertPruning(outlinkHistory + 361, 1_000_000, historyDifference + 361, 361, 1_000_000 - TRANSACTION_HASH_CACHE_HISTORY);
		assertPruning(outlinkHistory + 362, 1_000_000, 0, 0, -1);

		assertPruning(outlinkHistory + 720, 1_000_000,  0, 0, -1);
		assertPruning(outlinkHistory + 721, 1_000_000, historyDifference + 721, 721, 1_000_000 - TRANSACTION_HASH_CACHE_HISTORY);
		assertPruning(outlinkHistory + 722, 1_000_000, 0, 0, -1);
	}

	@Test
	public void pruneIsCalledWhenBlockHeightIsMuchGreaterThanHistories() {
		// Assert:
		assertPruning(
				10 * OUTLINK_BLOCK_HISTORY + 1,
				1_000_000,
				10 * OUTLINK_BLOCK_HISTORY - WEIGHTED_BALANCE_BLOCK_HISTORY + 1,
				10 * OUTLINK_BLOCK_HISTORY - OUTLINK_BLOCK_HISTORY + 1,
				1_000_000 - TRANSACTION_HASH_CACHE_HISTORY);
	}

	@Test
	public void outlinkPruningUsesOutlinkBlockHistoryOldBeforeBetaOutlinkPruningFork() {
		// Assert:
		final long notificationHeight = (BETA_OUTLINK_PRUNING_FORK / 360) * 360 + 1;
		final int notificationTime = 1_000_000;
		assertPruning(
				notificationHeight,
				notificationTime,
				notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY,
				notificationHeight - OUTLINK_BLOCK_HISTORY_OLD,
				notificationTime - TRANSACTION_HASH_CACHE_HISTORY);
	}

	@Test
	public void outlinkPruningUsesOutlinkBlockHistoryAfterBetaOutlinkPruningFork() {
		// Assert:
		final long notificationHeight = (BETA_OUTLINK_PRUNING_FORK / 360 + 1) * 360 + 1;
		final int notificationTime = 1_000_000;
		assertPruning(
				notificationHeight,
				notificationTime,
				notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY,
				notificationHeight - OUTLINK_BLOCK_HISTORY,
				notificationTime - TRANSACTION_HASH_CACHE_HISTORY);
	}

	private static void assertPruning(
			final long notificationHeight,
			final int notificationTime,
			final long expectedWeightedBalancePruningHeight,
			final long expectedOutlinkPruningHeight,
			final int expectedTransactionHashCachePruningTime) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Notification notification = createAdjustmentNotification(NotificationType.HarvestReward);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				new BlockHeight(notificationHeight),
				new TimeInstant(notificationTime),
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

		if (-1 != expectedTransactionHashCachePruningTime) {
			context.assertTransactionHashCachePruning(new TimeInstant(expectedTransactionHashCachePruningTime));
		} else {
			context.assertNoTransactionHashCachePruning();
		}
	}

	//endregion

	private static Notification createAdjustmentNotification(final NotificationType type) {
		return new BalanceAdjustmentNotification(type, Utils.generateRandomAccount(), Amount.ZERO);
	}

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final HashCache transactionHashCache = Mockito.mock(HashCache.class);
		private final List<PoiAccountState> accountStates = new ArrayList<>();
		private final BlockTransactionObserver observer = new PruningObserver(this.poiFacade, this.transactionHashCache);

		private TestContext() {
			for (int i = 0; i < 3; ++i) {
				final PoiAccountState accountState = Mockito.mock(PoiAccountState.class);
				final AccountImportance accountImportance = Mockito.mock(AccountImportance.class);
				final WeightedBalances weightedBalances = Mockito.mock(WeightedBalances.class);

				Mockito.when(accountState.getImportanceInfo()).thenReturn(accountImportance);
				Mockito.when(accountState.getWeightedBalances()).thenReturn(weightedBalances);
				this.accountStates.add(accountState);
			}

			Mockito.when(this.poiFacade.iterator()).thenReturn(this.accountStates.iterator());
		}

		private void assertNoPruning() {
			this.assertNoWeightedBalancePruning();
			this.assertNoOutlinkPruning();
			this.assertNoTransactionHashCachePruning();
		}

		private void assertNoWeightedBalancePruning() {
			for (final PoiAccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getWeightedBalances(), Mockito.never()).prune(Mockito.any());
			}
		}

		private void assertNoOutlinkPruning() {
			for (final PoiAccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getImportanceInfo(), Mockito.never()).prune(Mockito.any());
			}
		}

		private void assertNoTransactionHashCachePruning() {
			Mockito.verify(this.transactionHashCache, Mockito.never()).prune(Mockito.any());
		}

		private void assertWeightedBalancePruning(final BlockHeight height) {
			for (final PoiAccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getWeightedBalances(), Mockito.only()).prune(height);
			}
		}

		private void assertOutlinkPruning(final BlockHeight height) {
			for (final PoiAccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getImportanceInfo(), Mockito.only()).prune(height);
			}
		}

		private void assertTransactionHashCachePruning(final TimeInstant timeStamp) {
			Mockito.verify(this.transactionHashCache, Mockito.only()).prune(timeStamp);
		}
	}
}