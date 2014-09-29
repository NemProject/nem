package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

public class BlockNotificationContextTest {

	@Test
	public void canCreateContext() {
		// Act:
		final BlockNotificationContext context = new BlockNotificationContext(
				new BlockHeight(11),
				NotificationTrigger.Undo);

		// Assert:
		Assert.assertThat(context.getHeight(), IsEqual.equalTo(new BlockHeight(11)));
		Assert.assertThat(context.getTrigger(), IsEqual.equalTo(NotificationTrigger.Undo));
	}
}