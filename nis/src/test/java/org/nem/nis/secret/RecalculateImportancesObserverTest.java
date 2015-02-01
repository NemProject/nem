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
	public void recalculateImportancesIsNotCalledForOtherNotification() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, Utils.generateRandomAccount(), Amount.ZERO),
				NisUtils.createBlockNotificationContext(new BlockHeight(127), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.never()).recalculateImportances(Mockito.any(), Mockito.any());
		Mockito.verify(context.accountStateCache, Mockito.never()).mutableContents();
	}

	private static void assertImportanceRecalculation(
			final NotificationTrigger trigger,
			final BlockHeight height,
			final BlockHeight expectedRecalculateBlockHeight) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BlockHarvest, Utils.generateRandomAccount(), Amount.ZERO),
				NisUtils.createBlockNotificationContext(height, trigger));

		// Assert: recalculateImportances is called with grouped height
		Mockito.verify(context.poiFacade, Mockito.only()).recalculateImportances(Mockito.eq(expectedRecalculateBlockHeight), Mockito.any());
		Mockito.verify(context.accountStateCache, Mockito.only()).mutableContents();
	}

	private static class TestContext {
		private final DefaultPoiFacade poiFacade = Mockito.mock(DefaultPoiFacade.class);
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final NisCache nisCache = NisCacheFactory.create(this.accountStateCache, this.poiFacade);
		private final BlockTransactionObserver observer = new RecalculateImportancesObserver(this.nisCache);

		public TestContext() {
			Mockito.when(this.accountStateCache.mutableContents()).thenReturn(new CacheContents<>(new ArrayList<>()));
		}
	}
}