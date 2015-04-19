package org.nem.core.messages;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class MessageFactoryTest {

	@Test(expected = IllegalArgumentException.class)
	public void cannotDeserializeUnknownMessage() {
		// Arrange:
		final JSONObject object = new JSONObject();
		object.put("type", 7);
		final JsonDeserializer deserializer = new JsonDeserializer(object, null);

		// Act:
		deserialize(deserializer);
	}

	@Test
	public void canDeserializePlainMessage() {
		// Arrange:
		final PlainMessage originalMessage = new PlainMessage(new byte[] { 1, 2, 4 });
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, new MockAccountLookup());

		// Act:
		final Message message = deserialize(deserializer);

		// Assert:
		Assert.assertThat(message, IsInstanceOf.instanceOf(PlainMessage.class));
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.PLAIN));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 2, 4 }));
	}

	@Test
	public void canDeserializeSecureMessage() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();

		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(sender);
		accountLookup.setMockAccount(recipient);

		final SecureMessage originalMessage = SecureMessage.fromDecodedPayload(sender, recipient, new byte[] { 1, 2, 4 });
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, accountLookup);

		// Act:
		final Message message = MessageFactory.deserialize(deserializer, sender, recipient);

		// Assert:
		Assert.assertThat(message, IsInstanceOf.instanceOf(SecureMessage.class));
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 2, 4 }));
	}

	private static Message deserialize(final Deserializer deserializer) {
		return MessageFactory.deserialize(deserializer, Utils.generateRandomAccount(), Utils.generateRandomAccount());
	}
}
