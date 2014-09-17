package org.nem.core.model.observers;

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
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.Account));
		Assert.assertThat(notification.getAccount(), IsEqual.equalTo(account));
	}
}