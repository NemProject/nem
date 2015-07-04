package org.nem.core.model.observers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.test.Utils;

public class MosaicCreationNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Mosaic mosaic = Utils.createMosaic(Utils.generateRandomAccount());
		final MosaicCreationNotification notification = new MosaicCreationNotification(mosaic);

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.MosaicCreation));
		Assert.assertThat(notification.getMosaic(), IsSame.sameInstance(mosaic));
	}
}