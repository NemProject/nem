package org.nem.core.model.observers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class SmartTileTransferNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final SmartTileTransferNotification notification = new SmartTileTransferNotification(
				sender,
				recipient,
				Quantity.fromValue(123),
				Utils.createSmartTile(3));

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.SmartTileTransfer));
		Assert.assertThat(notification.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(notification.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(notification.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
		Assert.assertThat(notification.getSmartTile(), IsEqual.equalTo(Utils.createSmartTile(3)));
	}
}
