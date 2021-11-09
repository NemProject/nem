package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

public class HarvestRewardCommitObserverTest {

	// region execute

	@Test
	public void harvestRewardExecuteIncrementsHarvestedBlocks() {
		// Arrange:
		final TestContext context = new TestContext(Amount.fromNem(100), 3);

		// Act:
		context.notifyHarvestRewardExecute();

		// Assert:
		MatcherAssert.assertThat(context.accountInfo.getHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(4)));
	}

	@Test
	public void harvestRewardExecuteDoesNotChangeBalance() {
		// Arrange:
		final TestContext context = new TestContext(Amount.fromNem(100), 3);

		// Act:
		context.notifyHarvestRewardExecute();

		// Assert:
		MatcherAssert.assertThat(context.accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	// endregion

	// region undo

	@Test
	public void harvestRewardUndoDecrementsHarvestedBlocks() {
		// Arrange:
		final TestContext context = new TestContext(Amount.fromNem(100), 3);

		// Act:
		context.notifyHarvestRewardUndo();

		// Assert:
		MatcherAssert.assertThat(context.accountInfo.getHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	@Test
	public void harvestRewardUndoDoesNotChangeBalance() {
		// Arrange:
		final TestContext context = new TestContext(Amount.fromNem(100), 3);

		// Act:
		context.notifyHarvestRewardUndo();

		// Assert:
		MatcherAssert.assertThat(context.accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	// endregion

	// region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext(Amount.fromNem(100), 3);

		// Act:
		context.observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, context.account, Amount.fromNem(22)),
				NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));

		// Assert:
		MatcherAssert.assertThat(context.accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
		MatcherAssert.assertThat(context.accountInfo.getHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
	}

	// endregion

	private static class TestContext {
		private final Address address = Utils.generateRandomAddress();
		private final Account account = new Account(this.address);
		private final AccountInfo accountInfo = new AccountInfo();
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final HarvestRewardCommitObserver observer = new HarvestRewardCommitObserver(this.accountStateCache);

		public TestContext(final Amount amount, final int numHarvestedBlocks) {
			this.accountInfo.incrementBalance(amount);
			for (int i = 0; i < numHarvestedBlocks; ++i) {
				this.accountInfo.incrementHarvestedBlocks();
			}

			final AccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getAccountInfo()).thenReturn(this.accountInfo);
			Mockito.when(this.accountStateCache.findStateByAddress(this.address)).thenReturn(accountState);
		}

		private void notifyHarvestRewardExecute() {
			this.observer.notify(new BalanceAdjustmentNotification(NotificationType.BlockHarvest, this.account, Amount.fromNem(22)),
					NisUtils.createBlockNotificationContext(NotificationTrigger.Execute));
		}

		public void notifyHarvestRewardUndo() {
			this.observer.notify(new BalanceAdjustmentNotification(NotificationType.BlockHarvest, this.account, Amount.fromNem(22)),
					NisUtils.createBlockNotificationContext(NotificationTrigger.Undo));
		}
	}
}
