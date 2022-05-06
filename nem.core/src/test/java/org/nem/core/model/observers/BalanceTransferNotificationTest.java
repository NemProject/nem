package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class BalanceTransferNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final BalanceTransferNotification notification = new BalanceTransferNotification(sender, recipient, Amount.fromNem(123));

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceTransfer));
		MatcherAssert.assertThat(notification.getSender(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(notification.getRecipient(), IsEqual.equalTo(recipient));
		MatcherAssert.assertThat(notification.getAmount(), IsEqual.equalTo(Amount.fromNem(123)));
	}
}
