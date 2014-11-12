package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;

import java.util.*;

public class PruningObserverTest {
	private static final long PRUNE_INTERVAL = 360;

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
		for (int i=1; i<1000; i++) {
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
		assertAllAccountsArePruned(2880, 0, 0);
		assertAllAccountsArePruned(2881, 1, 1);

		assertAllAccountsArePruned(3240, 0, 0);
		assertAllAccountsArePruned(3241, 361, 1);
		assertAllAccountsArePruned(3242, 0, 0);

		assertAllAccountsArePruned(3600, 0, 0);
		assertAllAccountsArePruned(3601, 721, 1);
		assertAllAccountsArePruned(3602, 0, 0);
	}

	@Test
	public void allAccountsArePrunedWhenBlockHeightIsNearOutlinkBlockHistory() {
		// Assert:
		assertAllAccountsArePruned(43200, 0, 0);
		assertAllAccountsArePruned(43201, 40321, 1);

		assertAllAccountsArePruned(43560, 0, 0);
		assertAllAccountsArePruned(43561, 40681, 361);
		assertAllAccountsArePruned(43562, 0, 0);

		assertAllAccountsArePruned(43920, 0, 0);
		assertAllAccountsArePruned(43921, 41041, 721);
		assertAllAccountsArePruned(43922, 0, 0);
	}

	@Test
	public void allAccountsArePrunedWhenBlockHeightIsMuchGreaterThanHistories() {
		// Assert:
		assertAllAccountsArePruned(432001, 429121, 388801);
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