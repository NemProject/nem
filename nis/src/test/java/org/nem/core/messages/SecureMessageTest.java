package org.nem.core.messages;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Cipher;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;

public class SecureMessageTest {

	@Test
	public void canCreateMessageAroundDecodedPayload() {
		// Act:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final SecureMessage message = SecureMessage.fromDecodedPayload(sender, recipient, input);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
		Assert.assertThat(message.getEncodedPayload(), IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void canCreateMessageAroundEncodedPayload() {
		// Act:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] decodedBytes = new byte[] { 12, 46, 7, 43, 22, 15 };
		final Cipher cipher = new Cipher(sender.getKeyPair(), recipient.getKeyPair());
		final byte[] encodedBytes = cipher.encrypt(decodedBytes);

		final SecureMessage message = SecureMessage.fromEncodedPayload(sender, recipient, encodedBytes);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(decodedBytes));
		Assert.assertThat(message.getEncodedPayload(), IsEqual.equalTo(encodedBytes));
	}

	@Test
	public void messageCanBeRoundTripped() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final SecureMessage originalMessage = SecureMessage.fromDecodedPayload(sender, recipient, input);

		// Act:
		final SecureMessage message = createRoundTrippedMessage(originalMessage, sender, recipient);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
		Assert.assertThat(message.getEncodedPayload(), IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void encodedPayloadIsSerialized() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final SecureMessage originalMessage = SecureMessage.fromDecodedPayload(sender, recipient, input);

		JsonSerializer serializer = new JsonSerializer();
		originalMessage.serialize(serializer);
		JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Act:
		byte[] payload = deserializer.readBytes("payload");

		// Assert:
		Assert.assertThat(payload, IsNot.not(IsEqual.equalTo(input)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void secureMessageCannotBeCreatedAroundDecodedPayloadWithoutSenderPrivateKey() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };

		// Act:
		final Account senderPublicKeyOnly = Utils.createPublicOnlyKeyAccount(sender);
		SecureMessage.fromDecodedPayload(senderPublicKeyOnly, recipient, input);
	}

	@Test
	public void secureMessageCanBeCreatedAroundEncodedPayloadWithoutSenderPrivateKey() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };

		// Act:
		final Account senderPublicKeyOnly = Utils.createPublicOnlyKeyAccount(sender);
		final Message message = SecureMessage.fromEncodedPayload(senderPublicKeyOnly, recipient, input);

		// Assert:
		Assert.assertThat(message.getEncodedPayload(), IsEqual.equalTo(input));
	}

	@Test
	public void secureMessageCannotBeDecodedWithoutRecipientPrivateKey() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final SecureMessage originalMessage = SecureMessage.fromDecodedPayload(sender, recipient, input);

		// Act:
		final Account recipientPublicKeyOnly = Utils.createPublicOnlyKeyAccount(recipient);
		final SecureMessage message = createRoundTrippedMessage(originalMessage, sender, recipientPublicKeyOnly);

		// Assert:
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(false));
		Assert.assertThat(message.getDecodedPayload(), IsNull.nullValue());
		Assert.assertThat(message.getEncodedPayload(), IsNot.not(IsEqual.equalTo(input)));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account other = Utils.generateRandomAccount();
		final SecureMessage message = SecureMessage.fromEncodedPayload(sender, recipient, new byte[] { 12, 77, 56 });

		// Assert:
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(sender, recipient, new byte[] { 12, 77, 56 }),
				IsEqual.equalTo(message));
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(other, recipient, new byte[] { 12, 77, 56 }),
				IsNot.not(IsEqual.equalTo(message)));
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(sender, other, new byte[] { 12, 77, 56 }),
				IsNot.not(IsEqual.equalTo(message)));
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(sender, recipient, new byte[] { 12, 77 }),
				IsNot.not(IsEqual.equalTo(message)));
		Assert.assertThat(new PlainMessage(new byte[] { 12, 77, 56 }), IsNot.not((Object)IsEqual.equalTo(message)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(message)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account other = Utils.generateRandomAccount();
		final SecureMessage message = SecureMessage.fromEncodedPayload(sender, recipient, new byte[] { 12, 77, 56 });
		final int hashCode = message.hashCode();

		// Assert:
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(sender, recipient, new byte[] { 12, 77, 56 }).hashCode(),
				IsEqual.equalTo(hashCode));
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(other, recipient, new byte[] { 12, 77, 56 }).hashCode(),
				IsEqual.equalTo(hashCode));
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(sender, other, new byte[] { 12, 77, 56 }).hashCode(),
				IsEqual.equalTo(hashCode));
		Assert.assertThat(
				SecureMessage.fromEncodedPayload(sender, recipient, new byte[] { 12, 77 }).hashCode(),
				IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	private static SecureMessage createRoundTrippedMessage(
			final SecureMessage originalMessage,
			final Account sender,
			final Account recipient) {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(sender);
		accountLookup.setMockAccount(recipient);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, accountLookup);
		deserializer.readInt("type");
		return new SecureMessage(deserializer);
	}
}