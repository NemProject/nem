package org.nem.core.messages;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class PlainMessageTest {

	@Test
	public void ctorCanCreateMessage() {
		// Act:
		byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final PlainMessage message = new PlainMessage(input);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.PLAIN));
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
		Assert.assertThat(message.getEncodedPayload(), IsEqual.equalTo(input));
	}

	@Test
	public void messageCanBeRoundTripped() {
		// Arrange:
		byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final PlainMessage originalMessage = new PlainMessage(input);

		// Act:
		final PlainMessage message = createRoundTrippedMessage(originalMessage);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.PLAIN));
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
		Assert.assertThat(message.getEncodedPayload(), IsEqual.equalTo(input));
	}

	private static PlainMessage createRoundTrippedMessage(final PlainMessage originalMessage) {
		// Act:
		Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, null);
		deserializer.readInt("type");
		return new PlainMessage(deserializer);
	}
}