package org.nem.core.model;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.AccountAnalyzer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.mock;

public class AccountsHeightObserverTest {
	@Test
	public void observerDelegatesToAa() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = mock(AccountAnalyzer.class);
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifySend(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifySend(new BlockHeight(13), account1, Amount.fromNem(3));
		observer.notifyReceive(new BlockHeight(34), account2, Amount.fromNem(10));

		// Assert:
		Mockito.verify(accountAnalyzer, Mockito.times(2)).findByAddress(account1.getAddress());
		Mockito.verify(accountAnalyzer, Mockito.times(2)).addAccountToCache(account1.getAddress());

		Mockito.verify(accountAnalyzer, Mockito.times(1)).findByAddress(account2.getAddress());
		Mockito.verify(accountAnalyzer, Mockito.times(1)).addAccountToCache(account2.getAddress());
	}

	//region adding
	@Test
	public void sendAddsToAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final Account account1 = Utils.generateRandomAccount();
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifySend(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifySend(new BlockHeight(13), account1, Amount.fromNem(3));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), equalTo(1));
		final Account result = accountAnalyzer.findByAddress(account1.getAddress());
		Assert.assertThat(result.getHeight(), equalTo(new BlockHeight(12)));
	}

	@Test
	public void receiveAddsToAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final Account account1 = Utils.generateRandomAccount();
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), equalTo(1));
		final Account result = accountAnalyzer.findByAddress(account1.getAddress());
		Assert.assertThat(result.getHeight(), equalTo(new BlockHeight(12)));
	}
	//endregion

	//region
	@Test
	public void undoSendRemovesFromAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final Account account1 = Utils.generateRandomAccount();
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifySend(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifySend(new BlockHeight(13), account1, Amount.fromNem(3));
		observer.notifySendUndo(new BlockHeight(12), account1, Amount.fromNem(2));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), equalTo(0));
	}

	@Test
	public void undoReceiveRemovesFromAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final Account account1 = Utils.generateRandomAccount();
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));
		observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(2));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), equalTo(0));
	}

	@Test
	public void undoSendAtWrongHeightDoesNotRemoveFromAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final Account account1 = Utils.generateRandomAccount();
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifySend(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifySend(new BlockHeight(13), account1, Amount.fromNem(3));
		observer.notifySendUndo(new BlockHeight(13), account1, Amount.fromNem(2));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), equalTo(1));
	}

	@Test
	public void undoReceiveAtWrongHeightDoesNotRemoveFromAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final Account account1 = Utils.generateRandomAccount();
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));
		observer.notifyReceiveUndo(new BlockHeight(13), account1, Amount.fromNem(2));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), equalTo(1));
	}
	//endregion
}
