package org.nem.core.model.observers;

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
		final BalanceAdjustmentNotification notification = new BalanceAdjustmentNotification(
				NotificationType.BalanceCredit,
				account,
				Amount.fromNem(123));

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.BalanceCredit));
		Assert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
		Assert.assertThat(notification.getAmount(), IsEqual.equalTo(Amount.fromNem(123)));
	}
}