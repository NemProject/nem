package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;

public class BalanceAdjustmentNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account account = Utils.generateRandomAccount();
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account,
				Amount.fromNem(123));

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceCredit));
		MatcherAssert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
		MatcherAssert.assertThat(notification.getAmount(), IsEqual.equalTo(Amount.fromNem(123)));
	}
}
