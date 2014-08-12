package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.*;

public class AccountsHeightObserverTest {

	//region notifyReceive

	@Test
	public void notifyReceiveDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		context.setupAccount(account1);
		context.setupAccount(account2);

		// Act:
		context.observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		context.observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));
		context.observer.notifyReceive(new BlockHeight(34), account2, Amount.fromNem(10));

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(2)).findByAddress(account1.getAddress());
		Mockito.verify(context.accountCache, Mockito.times(1)).findByAddress(account2.getAddress());
	}

	@Test
	public void notifyReceiveDelegatesToPoiFacade() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		context.setupAccount(account1);
		context.setupAccount(account2);

		// Act:
		context.observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		context.observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));
		context.observer.notifyReceive(new BlockHeight(34), account2, Amount.fromNem(10));

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.times(2)).findStateByAddress(account1.getAddress());
		Mockito.verify(context.poiFacade, Mockito.times(1)).findStateByAddress(account2.getAddress());
	}

	@Test
	public void notifyReceiveSetsAccountHeightToHeightAtWhichAccountWasFirstSeen() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		context.setupAccount(account1);

		// Act:
		context.observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		context.observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));

		// Assert:
		final PoiAccountState state = context.poiFacade.findStateByAddress(account1.getAddress());
		Assert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(12)));
	}

	@Test
	public void notifyReceiveIncrementsReferenceCounter() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		context.setupAccount(account1);

		// Act:
		context.observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		context.observer.notifyReceive(new BlockHeight(13), account1, Amount.fromNem(3));

		// Assert:
		final Account cachedAccount = context.accountCache.findByAddress(account1.getAddress());
		Assert.assertThat(cachedAccount.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(2)));
	}

	//endregion

	//region notifyReceiveUndo

	@Test
	public void notifyReceiveUndoRemovesAccountWithMatchingHeightAndZeroReferenceCounterFromAccountAnalyzer() {
		// Assert:
		assertReceiveUndoRemovesAccount(12, 12);
	}

	@Test
	public void notifyReceiveUndoRemovesAccountWithNonMatchingHeightAndZeroReferenceCounterFromAccountAnalyzer() {
		// Assert: (the height doesn't have to match)
		assertReceiveUndoRemovesAccount(12, 15);
	}

	private static void assertReceiveUndoRemovesAccount(final int accountHeight, final int undoHeight) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(accountHeight);
		account1.incrementReferenceCount();

		// Act:
		context.observer.notifyReceiveUndo(new BlockHeight(undoHeight), account1, Amount.fromNem(2));

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).removeFromCache(account1.getAddress());
		Mockito.verify(context.poiFacade, Mockito.times(1)).removeFromCache(account1.getAddress());
	}

	@Test
	public void notifyReceiveUndoDoesNotRemoveAccountWithNonZeroReferenceCounterFromAccountAnalyzer() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(12);
		account1.incrementReferenceCount();
		account1.incrementReferenceCount();

		// Act:
		context.observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(4));

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(0)).removeFromCache(Mockito.any());
		Mockito.verify(context.poiFacade, Mockito.times(0)).removeFromCache(Mockito.any());
	}

	@Test
	public void multipleReceiveUndoWithinSameBlockArePossible() {
		// Arrange:
		final AccountCache accountCache = new AccountCache();
		final PoiFacade poiFacade = new PoiFacade(Mockito.mock(PoiImportanceGenerator.class));
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(accountCache, poiFacade);
		final AccountsHeightObserver observer = new AccountsHeightObserver(accountAnalyzer);
		final Account account1 = accountCache.addAccountToCache(Utils.generateRandomAddress());

		// Act:
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(2));
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(4));
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(6));
		observer.notifyReceive(new BlockHeight(12), account1, Amount.fromNem(7));
		observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(7));
		observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(6));
		observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(4));

		// Assert:
		Assert.assertThat(accountCache.size(), IsEqual.equalTo(1));

		// Act:
		observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(2));

		// Assert:
		Assert.assertThat(accountCache.size(), IsEqual.equalTo(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notifyReceiveUndoFailsIfThereIsNoMatchingAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(12);
		account1.incrementReferenceCount();

		// Act:
		context.observer.notifyReceiveUndo(new BlockHeight(12), Utils.generateRandomAccount(), Amount.fromNem(2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notifyReceiveUndoFailsIfMatchingAccountHasUnsetHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(0);
		account1.incrementReferenceCount();

		// Act:
		context.observer.notifyReceiveUndo(new BlockHeight(13), account1, Amount.fromNem(2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notifyReceiveUndoFailsIfReferenceCounterUnderflows() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(12);

		// Act:
		context.observer.notifyReceiveUndo(new BlockHeight(12), account1, Amount.fromNem(2));
	}

	//endregion

	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(this.accountCache, this.poiFacade);
		private final AccountsHeightObserver observer = new AccountsHeightObserver(this.accountAnalyzer);

		private void setupAccount(final Account account) {
			final Address address = account.getAddress();
			Mockito.when(this.accountCache.findByAddress(address)).thenReturn(account);
			Mockito.when(this.poiFacade.findStateByAddress(address)).thenReturn(new PoiAccountState(address));
		}

		private Account createAccountWithHeight(final int height) {
			final Account account = Utils.generateRandomAccount();
			this.setupAccount(account);

			if (height > 0)
				this.poiFacade.findStateByAddress(account.getAddress()).setHeight(new BlockHeight(height));

			return account;
		}
	}
}
