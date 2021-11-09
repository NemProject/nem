package org.nem.nis.secret;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.test.*;

import java.util.ArrayList;

public class RecalculateImportancesObserverTest {

	@Test
	public void recalculateImportancesIsCalledForHarvestRewardExecuteNotification() {
		// Assert:
		assertImportanceRecalculation(NotificationTrigger.Execute, new BlockHeight(127), new BlockHeight(128));
	}

	@Test
	public void recalculateImportancesIsCalledForHarvestRewardExecuteUndoNotification() {
		// Assert:
		assertImportanceRecalculation(NotificationTrigger.Undo, new BlockHeight(127), new BlockHeight(127));
	}

	@Test
	public void recalculateImportancesIsNotCalledIfImportancesAtGroupedHeightAreAvailable() {
		// Assert:
		assertNoImportanceRecalculation(NotificationTrigger.Execute, new BlockHeight(127));
		assertNoImportanceRecalculation(NotificationTrigger.Undo, new BlockHeight(127));
	}

	@Test
	public void recalculateImportancesIsNotCalledForOtherNotification() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, Utils.generateRandomAccount(), Amount.ZERO),
				NisUtils.createBlockNotificationContext(new BlockHeight(127), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.poxFacade, Mockito.never()).recalculateImportances(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountStateCache, Mockito.never()).mutableContents();
	}

	private static void assertImportanceRecalculation(final NotificationTrigger trigger, final BlockHeight height,
			final BlockHeight expectedRecalculateBlockHeight) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BlockHarvest, Utils.generateRandomAccount(), Amount.ZERO),
				NisUtils.createBlockNotificationContext(height, trigger));

		// Assert: recalculateImportances is called
		Mockito.verify(context.poxFacade, Mockito.times(1)).recalculateImportances(Mockito.eq(expectedRecalculateBlockHeight),
				Mockito.any());
		Mockito.verify(context.accountStateCache, Mockito.times(1)).mutableContents();
	}

	private static void assertNoImportanceRecalculation(final NotificationTrigger trigger, final BlockHeight height) {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.poxFacade.getLastRecalculationHeight()).thenReturn(BlockHeight.ONE);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BlockHarvest, Utils.generateRandomAccount(), Amount.ZERO),
				NisUtils.createBlockNotificationContext(height, trigger));

		// Assert: recalculateImportances is not called
		Mockito.verify(context.poxFacade, Mockito.never()).recalculateImportances(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountStateCache, Mockito.never()).mutableContents();
	}

	private static class TestContext {
		private final DefaultPoxFacade poxFacade = Mockito.mock(DefaultPoxFacade.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final NisCache nisCache = NisCacheFactory.create(this.accountStateCache, this.poxFacade);
		private final BlockTransactionObserver observer = new RecalculateImportancesObserver(this.nisCache);

		public TestContext() {
			Mockito.when(this.accountStateCache.mutableContents()).thenReturn(new CacheContents<>(new ArrayList<>()));
		}
	}
}
