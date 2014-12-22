package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

public class UnconfirmedBalancesObserverTest {

	@Test
	public void notifyTransferDelegatesToDebitAndCreditMethods() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notifyTransfer(context.sender, context.recipient, Amount.fromNem(8));

		// Assert:
		Mockito.verify(context.observer, Mockito.times(1)).notifyDebit(context.sender, Amount.fromNem(8));
		Mockito.verify(context.observer, Mockito.times(1)).notifyCredit(context.recipient, Amount.fromNem(8));
	}

	@Test
	public void notifyDebitDecreasesAccountsUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notifyDebit(context.sender, Amount.fromNem(8));

		// Assert:
		Assert.assertThat(context.observer.get(context.sender), IsEqual.equalTo(Amount.fromNem(2)));
	}

	@Test
	public void notifyCreditIncreasesAccountsUnconfirmedBalance() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.observer.notifyCredit(context.recipient, Amount.fromNem(8));

		// Assert:
		Assert.assertThat(context.observer.get(context.recipient), IsEqual.equalTo(Amount.fromNem(8)));
	}

	@Test
	public void clearCacheClearsCreditedAndDebitedAmounts() {
		// Arrange:
		final TestContext context = new TestContext();
		context.observer.notifyCredit(context.recipient, Amount.fromNem(17));
		context.observer.notifyDebit(context.recipient, Amount.fromNem(8));

		// Act:
		Assert.assertThat(context.observer.get(context.recipient), IsEqual.equalTo(Amount.fromNem(9)));
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
		context.observer.notifyDebit(context.sender, Amount.fromNem(8));
		context.decrementConfirmedBalance(context.sender, Amount.fromNem(5));

		// Assert:
		Assert.assertThat(context.observer.unconfirmedBalancesAreValid(), IsEqual.equalTo(false));
	}

	private static class TestContext {
		private final Account sender = new Account(Utils.generateRandomAddress());
		private final Account recipient = new Account(Utils.generateRandomAddress());
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final UnconfirmedBalancesObserver observer = Mockito.spy(new UnconfirmedBalancesObserver(accountStateCache));

		public TestContext() {
			this.hookAccount(sender, Amount.fromNem(10));
			this.hookAccount(recipient, Amount.fromNem(0));
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
