package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class BalanceCommitTransferObserverTest {

	@Test
	public void notifyTransferUpdatesAccountBalances() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount(Amount.fromNem(100));
		final Account recipient = Utils.generateRandomAccount(Amount.fromNem(100));
		final BalanceCommitTransferObserver observer = new BalanceCommitTransferObserver();

		// Act:
		observer.notifyTransfer(sender, recipient, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(sender.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
		Assert.assertThat(recipient.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyCreditUpdatesAccountBalances() {
		// Arrange:
		final Account account = Utils.generateRandomAccount(Amount.fromNem(100));
		final BalanceCommitTransferObserver observer = new BalanceCommitTransferObserver();

		// Act:
		observer.notifyCredit(account, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyDebitUpdatesAccountBalances() {
		// Arrange:
		final Account account = Utils.generateRandomAccount(Amount.fromNem(100));
		final BalanceCommitTransferObserver observer = new BalanceCommitTransferObserver();

		// Act:
		observer.notifyDebit(account, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
	}
}