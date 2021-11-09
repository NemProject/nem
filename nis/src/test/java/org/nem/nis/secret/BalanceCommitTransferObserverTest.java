package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.observers.AccountNotification;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

public class BalanceCommitTransferObserverTest {

	@Test
	public void notifyTransferUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo senderAccountInfo = context.add(sender, Amount.fromNem(100));
		final Account recipient = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo recipientAccountInfo = context.add(recipient, Amount.fromNem(100));

		// Act:
		NotificationUtils.notifyTransfer(context.observer, sender, recipient, Amount.fromNem(20));

		// Assert:
		MatcherAssert.assertThat(senderAccountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
		MatcherAssert.assertThat(recipientAccountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyCreditUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo accountInfo = context.add(account, Amount.fromNem(100));

		// Act:
		NotificationUtils.notifyCredit(context.observer, account, Amount.fromNem(20));

		// Assert:
		MatcherAssert.assertThat(accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyDebitUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo accountInfo = context.add(account, Amount.fromNem(100));

		// Act:
		NotificationUtils.notifyDebit(context.observer, account, Amount.fromNem(20));

		// Assert:
		MatcherAssert.assertThat(accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
	}

	@Test
	public void notifyOtherDoesNotUpdateAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyAccountInfo accountInfo = context.add(account, Amount.fromNem(100));

		// Act:
		context.observer.notify(new AccountNotification(account));

		// Assert:
		MatcherAssert.assertThat(accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	private static class TestContext {
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final BalanceCommitTransferObserver observer = new BalanceCommitTransferObserver(this.accountStateCache);

		public ReadOnlyAccountInfo add(final Account account, final Amount amount) {
			final AccountInfo accountInfo = new AccountInfo();
			accountInfo.incrementBalance(amount);

			final AccountState accountState = Mockito.mock(AccountState.class);
			Mockito.when(accountState.getAccountInfo()).thenReturn(accountInfo);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return accountInfo;
		}
	}
}
