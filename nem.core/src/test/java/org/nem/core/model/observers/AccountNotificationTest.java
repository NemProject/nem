package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;

public class AccountNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account account = Utils.generateRandomAccount();
		final AccountNotification notification = new AccountNotification(account);

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.Account));
		MatcherAssert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
	}
}
