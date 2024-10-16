package org.nem.core.model.observers;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

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
		MatcherAssert.assertThat(notificationCaptor.getAllValues().stream().map(Notification::getType).collect(Collectors.toList()),
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

		final BalanceTransferNotification notification = (BalanceTransferNotification) notificationCaptor.getValue();
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceTransfer));
		MatcherAssert.assertThat(notification.getSender(), IsEqual.equalTo(account2));
		MatcherAssert.assertThat(notification.getRecipient(), IsEqual.equalTo(account1));
		MatcherAssert.assertThat(notification.getAmount(), IsEqual.equalTo(amount));
	}

	@Test
	public void mosaicTransferAccountsAreReversed() {
		// Arrange:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		final ReverseTransactionObserver reverseObserver = new ReverseTransactionObserver(observer);

		// Act:
		final Account account1 = Utils.generateRandomAccount();
		final Account account2 = Utils.generateRandomAccount();
		reverseObserver.notify(new MosaicTransferNotification(account1, account2, Utils.createMosaicId(12), new Quantity(45)));
		reverseObserver.commit();

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.only()).notify(notificationCaptor.capture());

		final MosaicTransferNotification notification = (MosaicTransferNotification) notificationCaptor.getValue();
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.MosaicTransfer));
		MatcherAssert.assertThat(notification.getSender(), IsEqual.equalTo(account2));
		MatcherAssert.assertThat(notification.getRecipient(), IsEqual.equalTo(account1));
		MatcherAssert.assertThat(notification.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(12)));
		MatcherAssert.assertThat(notification.getQuantity(), IsEqual.equalTo(new Quantity(45)));
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

		final BalanceAdjustmentNotification notification = (BalanceAdjustmentNotification) notificationCaptor.getValue();
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(retypedType));
		MatcherAssert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
		MatcherAssert.assertThat(notification.getAmount(), IsEqual.equalTo(amount));
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

		final AccountNotification notification = (AccountNotification) notificationCaptor.getValue();
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.Account));
		MatcherAssert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
	}
}
