package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.*;

public class MessageTest {

	@Test
	public void ctorCanCreateMessage() {
		// Act:
		final MockMessage message = new MockMessage(121);

		// Assert:
		MatcherAssert.assertThat(message.getType(), IsEqual.equalTo(MockMessage.TYPE));
		MatcherAssert.assertThat(message.getCustomField(), IsEqual.equalTo(121));
	}

	@Test
	public void messageCanBeRoundTripped() {
		// Arrange:
		final MockMessage originalMessage = new MockMessage(121);

		// Act:
		final MockMessage message = createRoundTrippedMessage(originalMessage);

		// Assert:
		MatcherAssert.assertThat(message.getType(), IsEqual.equalTo(MockMessage.TYPE));
		MatcherAssert.assertThat(message.getCustomField(), IsEqual.equalTo(121));
	}

	private static MockMessage createRoundTrippedMessage(final MockMessage originalMessage) {
		// Act:
		return new MockMessage(Utils.roundtripSerializableEntity(originalMessage, null));
	}
}
