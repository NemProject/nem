package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.observers.AccountNotification;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

public class UnconfirmedBalancesObserverTest {

	@Test
	public void notifyTransferUpdatesSenderAndRecipientUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		NotificationUtils.notifyTransfer(context.observer, context.sender, context.recipient, Amount.fromNem(7));

		// Assert:
		Assert.assertThat(context.observer.get(context.sender), IsEqual.equalTo(Amount.fromNem(3)));
		Assert.assertThat(context.observer.get(context.recipient), IsEqual.equalTo(Amount.fromNem(7)));
	}

	@Test
	public void notifyDebitDecreasesAccountsUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		NotificationUtils.notifyDebit(context.observer, context.sender, Amount.fromNem(8));

		// Assert:
		Assert.assertThat(context.observer.get(context.sender), IsEqual.equalTo(Amount.fromNem(2)));
	}

	@Test
	public void notifyDebitCannotDecreaseUnconfirmedBalanceBelowZero() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		ExceptionAssert.assertThrows(
				v -> NotificationUtils.notifyDebit(context.observer, context.sender, Amount.fromNem(12)),
				IllegalArgumentException.class);
	}

	@Test
	public void notifyCreditIncreasesAccountsUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		NotificationUtils.notifyCredit(context.observer, context.recipient, Amount.fromNem(8));

		// Assert:
		Assert.assertThat(context.observer.get(context.recipient), IsEqual.equalTo(Amount.fromNem(8)));
	}

	@Test
	public void notifyCreditAndDebitChangesAreAppliedCumulativelyToUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		NotificationUtils.notifyCredit(context.observer, context.recipient, Amount.fromNem(17));
		NotificationUtils.notifyDebit(context.observer, context.recipient, Amount.fromNem(8));
		NotificationUtils.notifyCredit(context.observer, context.recipient, Amount.fromNem(12));
		NotificationUtils.notifyDebit(context.observer, context.recipient, Amount.fromNem(5));

		// Assert:
		Assert.assertThat(context.observer.get(context.recipient), IsEqual.equalTo(Amount.fromNem(16)));
	}

	@Test
	public void clearCacheClearsCreditedAndDebitedAmounts() {
		// Arrange:
		final TestContext context = new TestContext();
		NotificationUtils.notifyCredit(context.observer, context.recipient, Amount.fromNem(17));
		NotificationUtils.notifyDebit(context.observer, context.recipient, Amount.fromNem(8));

		// Act:
		context.observer.clearCache();

		// Assert:
		Assert.assertThat(context.observer.get(context.recipient), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void unconfirmedBalancesAreValidReturnsTrueIfAllUnconfirmedBalancesArePositiveOrZero() {
		// Arrange (sender has 10 NEM, recipient has 0 NEM):
		final TestContext context = new TestContext();

		// Assert:
		Assert.assertThat(context.observer.unconfirmedBalancesAreValid(), IsEqual.equalTo(true));
	}

	@Test
	public void unconfirmedBalancesAreValidReturnsFalseIfAtLeastOneUnconfirmedBalanceIsNegative() {
		// Arrange:
		final TestContext context = new TestContext();
		NotificationUtils.notifyDebit(context.observer, context.sender, Amount.fromNem(8));
		context.decrementConfirmedBalance(context.sender, Amount.fromNem(5));

		// Assert:
		Assert.assertThat(context.observer.unconfirmedBalancesAreValid(), IsEqual.equalTo(false));
	}

	@Test
	public void notifyOtherDoesNotUpdateAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notify(new AccountNotification(context.sender));

		// Assert:
		Assert.assertThat(context.observer.get(context.sender), IsEqual.equalTo(Amount.fromNem(10)));
		Assert.assertThat(context.observer.get(context.recipient), IsEqual.equalTo(Amount.ZERO));
	}

	private static class TestContext {
		private final Account sender = new Account(Utils.generateRandomAddress());
		private final Account recipient = new Account(Utils.generateRandomAddress());
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final UnconfirmedBalancesObserver observer = Mockito.spy(new UnconfirmedBalancesObserver(this.accountStateCache));

		public TestContext() {
			this.hookAccount(this.sender, Amount.fromNem(10));
			this.hookAccount(this.recipient, Amount.fromNem(0));
		}

		private void hookAccount(final Account account, final Amount amount) {
			final AccountState accountStateSender = Mockito.mock(AccountState.class);
			final AccountInfo accountInfoSender = new AccountInfo();
			accountInfoSender.incrementBalance(amount);
			Mockito.when(accountStateSender.getAccountInfo()).thenReturn(accountInfoSender);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountStateSender);
		}

		private void decrementConfirmedBalance(final Account account, final Amount amount) {
			this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo().decrementBalance(amount);
		}
	}
}
