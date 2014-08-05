package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.*;

public class MessageTest {

	@Test
	public void ctorCanCreateMessage() {
		// Act:
		final MockMessage message = new MockMessage(121);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MockMessage.TYPE));
		Assert.assertThat(message.getCustomField(), IsEqual.equalTo(121));
	}

	@Test
	public void messageCanBeRoundTripped() {
		// Arrange:
		final MockMessage originalMessage = new MockMessage(121);

		// Act:
		final MockMessage message = createRoundTrippedMessage(originalMessage);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MockMessage.TYPE));
		Assert.assertThat(message.getCustomField(), IsEqual.equalTo(121));
	}

	private static MockMessage createRoundTrippedMessage(final MockMessage originalMessage) {
		// Act:
		return new MockMessage(Utils.roundtripSerializableEntity(originalMessage, null));
	}
}
