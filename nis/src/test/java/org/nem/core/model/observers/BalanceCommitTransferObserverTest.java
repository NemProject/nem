package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;

public class BalanceCommitTransferObserverTest {

	@Test
	public void notifyTransferUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final AccountInfo senderAccountInfo = context.add(sender, Amount.fromNem(100));
		final Account recipient = Utils.generateRandomAccount();
		final AccountInfo recipientAccountInfo = context.add(recipient, Amount.fromNem(100));

		// Act:
		context.observer.notifyTransfer(sender, recipient, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(senderAccountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
		Assert.assertThat(recipientAccountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyCreditUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final AccountInfo accountInfo = context.add(account, Amount.fromNem(100));

		// Act:
		context.observer.notifyCredit(account, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(120)));
	}

	@Test
	public void notifyDebitUpdatesAccountBalances() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final AccountInfo accountInfo = context.add(account, Amount.fromNem(100));

		// Act:
		context.observer.notifyDebit(account, Amount.fromNem(20));

		// Assert:
		Assert.assertThat(accountInfo.getBalance(), IsEqual.equalTo(Amount.fromNem(80)));
	}

	private static class TestContext {
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final BalanceCommitTransferObserver observer = new BalanceCommitTransferObserver(this.poiFacade);

		public AccountInfo add(final Account account, final Amount amount) {
			final AccountInfo accountInfo = new AccountInfo();
			accountInfo.incrementBalance(amount);

			final PoiAccountState accountState = Mockito.mock(PoiAccountState.class);
			Mockito.when(accountState.getAccountInfo()).thenReturn(accountInfo);
			Mockito.when(this.poiFacade.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return accountInfo;
		}
	}
}