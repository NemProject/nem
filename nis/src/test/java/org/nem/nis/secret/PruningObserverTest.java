package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;

import java.util.*;

public class PruningObserverTest {

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
		assertAllAccountsArePruned(2879, 1, 1);
		assertAllAccountsArePruned(2880, 1, 1);
		assertAllAccountsArePruned(2881, 1, 1);
		assertAllAccountsArePruned(2882, 2, 1);
		assertAllAccountsArePruned(2883, 3, 1);
	}

	@Test
	public void allAccountsArePrunedWhenBlockHeightIsNearOutlinkBlockHistory() {
		// Assert:
		assertAllAccountsArePruned(43199, 40319, 1);
		assertAllAccountsArePruned(43200, 40320, 1);
		assertAllAccountsArePruned(43201, 40321, 1);
		assertAllAccountsArePruned(43202, 40322, 2);
		assertAllAccountsArePruned(43203, 40323, 3);
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
		context.assertWeightedBalancePruning(new BlockHeight(expectedWeightedBalancePruningHeight));
		context.assertOutlinkPruning(new BlockHeight(expectedOutlinkPruningHeight));
	}

	//endregion

	private static Notification createAdjustmentNotification(final NotificationType type) {
		return new BalanceAdjustmentNotification(type, Utils.generateRandomAccount(), Amount.ZERO);
	}

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final List<PoiAccountState> accountStates =  new ArrayList<>();
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
			for (final PoiAccountState accountState : this.accountStates) {
				Mockito.verify(accountState.getWeightedBalances(), Mockito.never()).prune(Mockito.any());
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