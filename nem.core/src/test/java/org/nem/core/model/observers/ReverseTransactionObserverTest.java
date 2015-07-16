package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ReverseTransactionObserverTest {

	@Test
	public void notificationsAreCommittedInReverseOrder() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final ReverseTransactionObserver reverseObserver = new ReverseTransactionObserver(observer);

		// Act:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(12345);
		reverseObserver.notify(new BalanceTransferNotification(account1, account2, amount));
		reverseObserver.notify(new AccountNotification(account1));
		reverseObserver.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account1, amount));
		reverseObserver.commit();

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.atLeastOnce()).notify(notificationCaptor.capture());
		Assert.assertThat(
				notificationCaptor.getAllValues().stream().map(Notification::getType).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(NotificationType.BalanceCredit, NotificationType.Account, NotificationType.BalanceTransfer)));
	}

	@Test
	public void balanceTransferAccountsAreReversed() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final ReverseTransactionObserver reverseObserver = new ReverseTransactionObserver(observer);

		// Act:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(12345);
		reverseObserver.notify(new BalanceTransferNotification(account1, account2, amount));
		reverseObserver.commit();

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());

		final BalanceTransferNotification notification = (BalanceTransferNotification)notificationCaptor.getValue();
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceTransfer));
		Assert.assertThat(notification.getSender(), IsEqual.equalTo(account2));
		Assert.assertThat(notification.getRecipient(), IsEqual.equalTo(account1));
		Assert.assertThat(notification.getAmount(), IsEqual.equalTo(amount));
	}

	@Test
	public void smartTileTransferAccountsAreReversed() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final ReverseTransactionObserver reverseObserver = new ReverseTransactionObserver(observer);

		// Act:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		final SmartTile smartTile = Utils.createSmartTile(12);
		reverseObserver.notify(new SmartTileTransferNotification(account1, account2, smartTile));
		reverseObserver.commit();

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());

		final SmartTileTransferNotification notification = (SmartTileTransferNotification)notificationCaptor.getValue();
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.SmartTileTransfer));
		Assert.assertThat(notification.getSender(), IsEqual.equalTo(account2));
		Assert.assertThat(notification.getRecipient(), IsEqual.equalTo(account1));
		Assert.assertThat(notification.getSmartTile(), IsEqual.equalTo(smartTile));
	}

	@Test
	public void balanceCreditIsRetypedAsBalanceDebit() {
		// Assert:
		assertRetypedBalanceAdjustment(NotificationType.BalanceCredit, NotificationType.BalanceDebit);
	}

	@Test
	public void balanceDebitIsRetypedAsBalanceCredit() {
		// Assert:
		assertRetypedBalanceAdjustment(NotificationType.BalanceDebit, NotificationType.BalanceCredit);
	}

	private static void assertRetypedBalanceAdjustment(final NotificationType originalType, final NotificationType retypedType) {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final ReverseTransactionObserver reverseObserver = new ReverseTransactionObserver(observer);

		// Act:
		final Account account = Utils.generateRandomAccount();
		final Amount amount = Amount.fromNem(12345);
		reverseObserver.notify(new BalanceAdjustmentNotification(originalType, account, amount));
		reverseObserver.commit();

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());

		final BalanceAdjustmentNotification notification = (BalanceAdjustmentNotification)notificationCaptor.getValue();
		Assert.assertThat(notification.getType(), IsEqual.equalTo(retypedType));
		Assert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
		Assert.assertThat(notification.getAmount(), IsEqual.equalTo(amount));
	}

	@Test
	public void nonBalanceNotificationsAreLeftUnchanged() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final ReverseTransactionObserver reverseObserver = new ReverseTransactionObserver(observer);

		// Act:
		final Account account = Utils.generateRandomAccount();
		reverseObserver.notify(new AccountNotification(account));
		reverseObserver.commit();

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());

		final AccountNotification notification = (AccountNotification)notificationCaptor.getValue();
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.Account));
		Assert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
	}
}