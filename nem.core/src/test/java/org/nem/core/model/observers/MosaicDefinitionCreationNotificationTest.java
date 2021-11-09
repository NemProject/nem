package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.test.Utils;

public class MosaicDefinitionCreationNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(Utils.generateRandomAccount());
		final MosaicDefinitionCreationNotification notification = new MosaicDefinitionCreationNotification(mosaicDefinition);

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.MosaicDefinitionCreation));
		MatcherAssert.assertThat(notification.getMosaicDefinition(), IsSame.sameInstance(mosaicDefinition));
	}
}
