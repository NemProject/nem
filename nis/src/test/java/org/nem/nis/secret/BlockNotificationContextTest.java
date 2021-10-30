package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;

public class BlockNotificationContextTest {

	@Test
	public void canCreateContext() {
		// Act:
		final BlockNotificationContext context = new BlockNotificationContext(new BlockHeight(11), new TimeInstant(123),
				NotificationTrigger.Undo);

		// Assert:
		MatcherAssert.assertThat(context.getHeight(), IsEqual.equalTo(new BlockHeight(11)));
		MatcherAssert.assertThat(context.getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		MatcherAssert.assertThat(context.getTrigger(), IsEqual.equalTo(NotificationTrigger.Undo));
	}
}
