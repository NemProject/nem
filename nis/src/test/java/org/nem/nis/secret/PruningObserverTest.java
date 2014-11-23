package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.*;
import org.nem.nis.poi.*;

import java.util.*;

public class PruningObserverTest {
	private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY = 31 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY_OLD = 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long PRUNE_INTERVAL = 360;
	private static final long BETA_OUTLINK_PRUNING_FORK = BlockMarkerConstants.BETA_OUTLINK_PRUNING_FORK;

	//region no-op

	@Test
	public void noAccountsArePrunedWhenNotificationTriggerIsNotExecute() {
		// Assert:
		assertNoAccountsArePruned(432001, NotificationTrigger.Undo, NotificationType.HarvestReward);
	}

	@Test
	public void noAccountsArePrunedWhenNotificationTypeIsNotHarvestReward() {
		// Assert:
		assertNoAccountsArePruned(432001, NotificationTrigger.Execute, NotificationType.BalanceCredit);
	}

	@Test
	public void noAccountsArePrunedIfBlockHeightModuloThreeHundredSixtyIsNotOne() {
		// Assert:
		for (int i = 1; i < 1000; ++i) {
			if (1 != (i % PRUNE_INTERVAL)) {
				assertNoAccountsArePruned(i, NotificationTrigger.Execute, NotificationType.HarvestReward);
			}
		}
	}

	private static void assertNoAccountsArePruned(
			final long notificationHeight,
			final NotificationTrigger notificationTrigger,
			final NotificationType notificationType) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Notification notification = createAdjustmentNotification(notificationType);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				new BlockHeight(notificationHeight),
				notificationTrigger);
		context.observer.notify(notification, notificationContext);

		// Assert:
		context.assertNoPruning();
	}

	//endregion

	//region pruning trigger

	@Test
	public void allAccountsArePrunedAtInitialBlockHeight() {
		// Assert:
		assertAllAccountsArePruned(1, 1, 1);
	}

	@Test
	public void allAccountsArePrunedWhenBlockHeightIsNearWeightedBalanceBlockHistory() {
		// Assert:
		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY, 0, 0);
		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY + 1, 1, 1);

		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL, 0, 0);
		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 1, 361, 1);
		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 2, 0, 0);

		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL, 0, 0);
		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 1, 721, 1);
		assertAllAccountsArePruned(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 2, 0, 0);
	}

	@Test
	public void allAccountsArePrunedWhenBlockHeightIsNearOutlinkBlockHistory() {
		// Arrange:
		// TODO: Replace with new constant when launching.
		final long outlinkHistory = OUTLINK_BLOCK_HISTORY_OLD;
		final long historyDifference = OUTLINK_BLOCK_HISTORY_OLD - WEIGHTED_BALANCE_BLOCK_HISTORY;

		// Assert:
		assertAllAccountsArePruned(outlinkHistory, 0, 0);
		assertAllAccountsArePruned(outlinkHistory + 1, historyDifference + 1, 1);

		assertAllAccountsArePruned(outlinkHistory + 360, 0, 0);
		assertAllAccountsArePruned(outlinkHistory + 361, historyDifference + 361, 361);
		assertAllAccountsArePruned(outlinkHistory + 362, 0, 0);

		assertAllAccountsArePruned(outlinkHistory + 720, 0, 0);
		assertAllAccountsArePruned(outlinkHistory + 721, historyDifference + 721, 721);
		assertAllAccountsArePruned(outlinkHistory + 722, 0, 0);
	}

	@Test
	public void allAccountsArePrunedWhenBlockHeightIsMuchGreaterThanHistories() {
		// Assert:
		assertAllAccountsArePruned(
				10 * OUTLINK_BLOCK_HISTORY + 1,
				10 * OUTLINK_BLOCK_HISTORY - WEIGHTED_BALANCE_BLOCK_HISTORY + 1,
				10 * OUTLINK_BLOCK_HISTORY - OUTLINK_BLOCK_HISTORY + 1);
	}

	@Test
	public void outlinkPruningUsesOutlinkBlockHistoryOldBeforeBetaOutlinkPruningFork() {
		// Assert:
		final long notificationHeight = (BETA_OUTLINK_PRUNING_FORK / 360) * 360 + 1;
		assertAllAccountsArePruned(
				notificationHeight,
				notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY,
				notificationHeight - OUTLINK_BLOCK_HISTORY_OLD);
	}

	@Test
	public void outlinkPruningUsesOutlinkBlockHistoryAfterBetaOutlinkPruningFork() {
		// Assert:
		final long notificationHeight = (BETA_OUTLINK_PRUNING_FORK / 360 + 1) * 360 + 1;
		assertAllAccountsArePruned(
				notificationHeight,
				notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY,
				notificationHeight - OUTLINK_BLOCK_HISTORY);
	}

	private static void assertAllAccountsArePruned(
			final long notificationHeight,
			final long expectedWeightedBalancePruningHeight,
			final long expectedOutlinkPruningHeight) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Notification notification = createAdjustmentNotification(NotificationType.HarvestReward);
		final BlockNotificationContext notificationContext = new BlockNotificationContext(
				new BlockHeight(notificationHeight),
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

	private static Notification createAdjustmentNotification(final NotificationType type) {
		return new BalanceAdjustmentNotification(type, Utils.generateRandomAccount(), Amount.ZERO);
	}

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final List<PoiAccountState> accountStates = new ArrayList<>();
		private final BlockTransactionObserver observer = new PruningObserver(this.poiFacade);

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
	}
}