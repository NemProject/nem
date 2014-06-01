package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.Utils;
import org.nem.nis.AccountAnalyzer;

public class AccountsHeightObserverTest {

	@Test
	public void notifyReceiveDelegatesToAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		Mockito.when(accountAnalyzer.findByAddress(account1.getAddress())).thenReturn(account1);
		Mockito.when(accountAnalyzer.findByAddress(account2.getAddress())).thenReturn(account2);

		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));
		observer.notifyReceive(new BlockHeight(34), account2, Amount.fromNem(10));

		// Assert:
		Mockito.verify(accountAnalyzer, Mockito.times(2)).findByAddress(account1.getAddress());
		Mockito.verify(accountAnalyzer, Mockito.times(1)).findByAddress(account2.getAddress());
	}

	@Test
	public void notifyReceiveSetsAccountHeightToHeightAtWhichAccountWasFirstSeen() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final Account account1 = Utils.generateRandomAccount();
		accountAnalyzer.addAccountToCache(account1.getAddress());
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), IsEqual.equalTo(1));
		final Account result = accountAnalyzer.findByAddress(account1.getAddress());
		Assert.assertThat(result.getHeight(), IsEqual.equalTo(new BlockHeight(12)));
	}

	//endregion

	//region notifyReceiveUndo

	@Test
	public void notifyReceiveUndoRemovesAccountWithMatchingHeightFromAccountAnalyzer() {
		// Arrange:
		final Account account1 = createAccountWithHeight(12);
		final AccountAnalyzer accountAnalyzer = createAccountAnalyzerWithAccount(account1);
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(2));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), IsEqual.equalTo(0));
	}

	@Test
	public void notifyReceiveUndoDoesNotRemoveAccountWithNonMatchingHeightFromAccountAnalyzer() {
		// Arrange:
		final Account account1 = createAccountWithHeight(12);
		final AccountAnalyzer accountAnalyzer = createAccountAnalyzerWithAccount(account1);
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceiveUndo(new BlockHeight(13), account1, Amount.fromNem(2));

		// Assert:
		Assert.assertThat(accountAnalyzer.size(), IsEqual.equalTo(1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notifyReceiveUndoFailsIfThereIsNoMatchingAccount() {
		// Arrange:
		final Account account1 = createAccountWithHeight(12);
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);

		// Act:
		observer.notifyReceiveUndo(new BlockHeight(13), account1, Amount.fromNem(2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notifyReceiveUndoFailsIfMatchingAccountHasUnsetHeight() {
		// Arrange:
		final Account account1 = Utils.generateRandomAccount();
		final AccountAnalyzer accountAnalyzer = createAccountAnalyzerWithAccount(account1);
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);
		accountAnalyzer.addAccountToCache(account1.getAddress());

		// Act:
		observer.notifyReceiveUndo(new BlockHeight(13), account1, Amount.fromNem(2));
	}

	private static Account createAccountWithHeight(final int height) {
		final Account account = Utils.generateRandomAccount();
		account.setHeight(new BlockHeight(height));
		return account;
	}

	private static AccountAnalyzer createAccountAnalyzerWithAccount(final Account account) {
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(null);
		accountAnalyzer.addAccountToCache(account.getAddress());
		accountAnalyzer.findByAddress(account.getAddress()).setHeight(account.getHeight());
		return accountAnalyzer;
	}

	//endregion
}
