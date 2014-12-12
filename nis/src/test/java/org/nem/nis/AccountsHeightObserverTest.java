package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

public class AccountsHeightObserverTest {

	//region AccountNotification / NotificationTrigger.Execute

	@Test
	public void accountNotificationExecuteDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		context.setupAccount(account1);
		context.setupAccount(account2);

		// Act:
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(13));
		context.observer.notify(new AccountNotification(account2), createExecuteNotificationContext(34));

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(2)).findByAddress(account1.getAddress());
		Mockito.verify(context.accountCache, Mockito.times(1)).findByAddress(account2.getAddress());
	}

	@Test
	public void accountNotificationExecuteDelegatesToPoiFacade() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		context.setupAccount(account1);
		context.setupAccount(account2);

		// Act:
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(13));
		context.observer.notify(new AccountNotification(account2), createExecuteNotificationContext(34));

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.times(2)).findStateByAddress(account1.getAddress());
		Mockito.verify(context.poiFacade, Mockito.times(1)).findStateByAddress(account2.getAddress());
	}

	@Test
	public void accountNotificationExecuteSetsAccountHeightToHeightAtWhichAccountWasFirstSeen() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		context.setupAccount(account1);

		// Act:
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(13));

		// Assert:
		final AccountState state = context.poiFacade.findStateByAddress(account1.getAddress());
		Assert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(12)));
	}

	@Test
	public void accountNotificationExecuteIncrementsReferenceCounter() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();
		context.setupAccount(account1);

		// Act:
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		context.observer.notify(new AccountNotification(account1), createExecuteNotificationContext(13));

		// Assert:
		final ReadOnlyAccountInfo accountInfo = context.poiFacade.findStateByAddress(account1.getAddress()).getAccountInfo();
		Assert.assertThat(accountInfo.getReferenceCount(), IsEqual.equalTo(new ReferenceCount(2)));
	}

	//endregion

	//region AccountNotification / NotificationTrigger.Undo

	@Test
	public void accountNotificationUndoRemovesAccountWithMatchingHeightAndZeroReferenceCounterFromAccountAnalyzer() {
		// Assert:
		assertAccountNotificationUndoRemovesAccount(12, 12);
	}

	@Test
	public void accountNotificationUndoRemovesAccountWithNonMatchingHeightAndZeroReferenceCounterFromAccountAnalyzer() {
		// Assert: (the height doesn't have to match)
		assertAccountNotificationUndoRemovesAccount(12, 15);
	}

	private static void assertAccountNotificationUndoRemovesAccount(final int accountHeight, final int undoHeight) {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(accountHeight);
		final AccountInfo accountInfo = context.poiFacade.findStateByAddress(account1.getAddress()).getAccountInfo();
		accountInfo.incrementReferenceCount();

		// Act:
		context.observer.notify(new AccountNotification(account1), createUndoNotificationContext(undoHeight));

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).removeFromCache(account1.getAddress());
		Mockito.verify(context.poiFacade, Mockito.times(1)).removeFromCache(account1.getAddress());
	}

	@Test
	public void accountNotificationUndoDoesNotRemoveAccountWithNonZeroReferenceCounterFromAccountAnalyzer() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(12);
		final AccountInfo accountInfo = context.poiFacade.findStateByAddress(account1.getAddress()).getAccountInfo();
		accountInfo.incrementReferenceCount();
		accountInfo.incrementReferenceCount();

		// Act:
		context.observer.notify(new AccountNotification(account1), createUndoNotificationContext(12));

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(0)).removeFromCache(Mockito.any());
		Mockito.verify(context.poiFacade, Mockito.times(0)).removeFromCache(Mockito.any());
	}

	@Test
	public void multipleReceiveUndoWithinSameBlockArePossible() {
		// Arrange:
		final AccountCache accountCache = new AccountCache();
		final AccountStateRepository poiFacade = new DefaultAccountStateRepository();
		final AccountsHeightObserver observer = new AccountsHeightObserver(NisCacheFactory.create(accountCache, poiFacade));
		final Account account1 = accountCache.addAccountToCache(Utils.generateRandomAddress());

		// Act:
		observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		observer.notify(new AccountNotification(account1), createExecuteNotificationContext(12));
		observer.notify(new AccountNotification(account1), createUndoNotificationContext(12));
		observer.notify(new AccountNotification(account1), createUndoNotificationContext(12));
		observer.notify(new AccountNotification(account1), createUndoNotificationContext(12));

		// Assert:
		Assert.assertThat(accountCache.size(), IsEqual.equalTo(1));

		// Act:
		observer.notify(new AccountNotification(account1), createUndoNotificationContext(12));

		// Assert:
		Assert.assertThat(accountCache.size(), IsEqual.equalTo(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountNotificationUndoFailsIfThereIsNoMatchingAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(12);
		final AccountInfo accountInfo = context.poiFacade.findStateByAddress(account1.getAddress()).getAccountInfo();
		accountInfo.incrementReferenceCount();

		// Act:
		context.observer.notify(new AccountNotification(Utils.generateRandomAccount()), createUndoNotificationContext(12));
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountNotificationUndoFailsIfMatchingAccountHasUnsetHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(0);
		final AccountInfo accountInfo = context.poiFacade.findStateByAddress(account1.getAddress()).getAccountInfo();
		accountInfo.incrementReferenceCount();

		// Act:
		context.observer.notify(new AccountNotification(account1), createUndoNotificationContext(13));
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountNotificationUndoFailsIfReferenceCounterUnderflows() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = context.createAccountWithHeight(12);

		// Act:
		context.observer.notify(new AccountNotification(account1), createUndoNotificationContext(12));
	}

	//endregion

	//region Other Notification

	@Test
	public void nonAccountNotificationsAreIgnored() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account1 = Utils.generateRandomAccount();

		// Act:
		context.observer.notify(
				new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account1, Amount.fromNem(12)),
				createExecuteNotificationContext(12));

		// Assert:
		Mockito.verifyZeroInteractions(context.accountCache, context.poiFacade);
	}

	//endregion

	private static BlockNotificationContext createExecuteNotificationContext(final int height) {
		return NisUtils.createBlockNotificationContext(new BlockHeight(height), NotificationTrigger.Execute);
	}

	private static BlockNotificationContext createUndoNotificationContext(final int height) {
		return NisUtils.createBlockNotificationContext(new BlockHeight(height), NotificationTrigger.Undo);
	}

	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final AccountStateRepository poiFacade = Mockito.mock(AccountStateRepository.class);
		private final NisCache nisCache = NisCacheFactory.create(this.accountCache, this.poiFacade);
		private final AccountsHeightObserver observer = new AccountsHeightObserver(this.nisCache);

		private void setupAccount(final Account account) {
			final Address address = account.getAddress();
			Mockito.when(this.accountCache.findByAddress(address)).thenReturn(account);
			Mockito.when(this.poiFacade.findStateByAddress(address)).thenReturn(new AccountState(address));
		}

		private Account createAccountWithHeight(final int height) {
			final Account account = Utils.generateRandomAccount();
			this.setupAccount(account);

			if (height > 0) {
				this.poiFacade.findStateByAddress(account.getAddress()).setHeight(new BlockHeight(height));
			}

			return account;
		}
	}
}
