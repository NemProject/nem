package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.*;
import org.nem.nis.poi.*;

public class HarvestRewardCommitObserverTest {

	//region local harvesting

	@Test
	public void localHarvestingExecuteCanBeCommitted() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, account, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Execute));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(122)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(4)));
	}

	@Test
	public void localHarvestingUndoCanBeCommitted() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, account, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Undo));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(78)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	//endregion

	//region local harvesting

	@Test
	public void remoteHarvestingExecuteCanBeCommitted() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);
		final Account remoteAccount = context.createRemoteAccount(account);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, remoteAccount, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Execute));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(122)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(4)));
	}

	@Test
	public void remoteHarvestingUndoCanBeCommitted() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);
		final Account remoteAccount = context.createRemoteAccount(account);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, remoteAccount, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Undo));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(78)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	//endregion

	//region delegation

	@Test
	public void observerDelegatesStateLookupToPoiFacade() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, account, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.times(1)).findForwardedStateByAddress(account.getAddress(), new BlockHeight(4));
	}

	@Test
	public void observerDelegatesAccountLookupToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);
		final Account remoteAccount = context.createRemoteAccount(account);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, remoteAccount, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Execute));

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).findByAddress(account.getAddress());
	}

	//endregion

	//region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = context.createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Execute));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
	}

	//endregion

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final BlockTransactionObserver observer = new HarvestRewardCommitObserver(this.poiFacade, this.accountCache);

		private Account createAccountWithBalanceAndBlocks(final Amount balance, final int numHarvestedBlocks) {
			final Account account = Utils.generateRandomAccount();
			account.incrementBalance(balance);
			for (int i = 0; i < numHarvestedBlocks; ++i) {
				account.incrementForagedBlocks();
			}

			final PoiAccountState accountState = new PoiAccountState(account.getAddress());
			accountState.getWeightedBalances().addReceive(BlockHeight.ONE, balance);
			Mockito.when(this.poiFacade.findStateByAddress(account.getAddress())).thenReturn(accountState);
			Mockito.when(this.poiFacade.findForwardedStateByAddress(Mockito.eq(account.getAddress()), Mockito.any())).thenReturn(accountState);
			Mockito.when(this.accountCache.findByAddress(account.getAddress())).thenReturn(account);
			return account;
		}

		private Account createRemoteAccount(final Account forwardedAccount) {
			final Account account = createAccountWithBalanceAndBlocks(Amount.ZERO, 0);

			final PoiAccountState forwardedAccountState = new PoiAccountState(forwardedAccount.getAddress());
			Mockito.when(this.poiFacade.findForwardedStateByAddress(Mockito.eq(account.getAddress()), Mockito.any())).thenReturn(forwardedAccountState);
			return account;
		}
	}
}