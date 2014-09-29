package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class HarvestRewardCommitObserverTest {
	private static final BlockTransactionObserver OBSERVER = new HarvestRewardCommitObserver();

	//region execute

	@Test
	public void harvestRewardExecuteIncrementsForagedBlocks() {
		// Arrange:
		final Account account = createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		notifyHarvestRewardExecute(account);

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(4)));
	}

	@Test
	public void harvestRewardExecuteDoesNotChangeBalance() {
		// Arrange:
		final Account account = createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		notifyHarvestRewardExecute(account);

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	private static void notifyHarvestRewardExecute(final Account account) {
		OBSERVER.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, account, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Execute));
	}

	//endregion

	//region undo

	@Test
	public void harvestRewardUndoDecrementsForagedBlocks() {
		// Arrange:
		final Account account = createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		notifyHarvestRewardUndo(account);

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	@Test
	public void harvestRewardUndoDoesNotChangeBalance() {
		// Arrange:
		final Account account = createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		notifyHarvestRewardUndo(account);

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	private static void notifyHarvestRewardUndo(final Account account) {
		OBSERVER.notify(
				new BalanceAdjustmentNotification(NotificationType.HarvestReward, account, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Undo));
	}

	//endregion

	//region other types

	@Test
	public void otherNotificationTypesAreIgnored() {
		// Arrange:
		final Account account = createAccountWithBalanceAndBlocks(Amount.fromNem(100), 3);

		// Act:
		OBSERVER.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, Amount.fromNem(22)),
				new BlockNotificationContext(new BlockHeight(4), NotificationTrigger.Execute));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
	}

	//endregion

	private static Account createAccountWithBalanceAndBlocks(final Amount balance, final int numHarvestedBlocks) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(balance);
		for (int i = 0; i < numHarvestedBlocks; ++i) {
			account.incrementForagedBlocks();
		}

		return account;
	}
}