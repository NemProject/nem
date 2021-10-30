package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.Utils;

public class MosaicTransferNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final MosaicTransferNotification notification = new MosaicTransferNotification(sender, recipient, Utils.createMosaicId(3),
				new Quantity(467));

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.MosaicTransfer));
		MatcherAssert.assertThat(notification.getSender(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(notification.getRecipient(), IsEqual.equalTo(recipient));
		MatcherAssert.assertThat(notification.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(3)));
		MatcherAssert.assertThat(notification.getQuantity(), IsEqual.equalTo(new Quantity(467)));
	}
}
