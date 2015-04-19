package org.nem.core.messages;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class SecureMessageTest {

	//region construction

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
	public void canCreateMessageAroundEmptyDecodedPayload() {
		// Act:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final byte[] input = new byte[] {};
		final SecureMessage message = SecureMessage.fromDecodedPayload(sender, recipient, input);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
		Assert.assertThat(message.getEncodedPayload(), IsNot.not(IsEqual.equalTo(input)));
		Assert.assertThat(message.getEncodedPayload().length, IsEqual.equalTo(64));
	}

	@Test
	public void canCreateMessageAroundEncodedPayload() {
		// Act:
		final KeyPair senderKeyPair = new KeyPair();
		final KeyPair recipientKeyPair = new KeyPair();
		final byte[] decodedBytes = new byte[] { 12, 46, 7, 43, 22, 15 };
		final Cipher cipher = new Cipher(senderKeyPair, recipientKeyPair);
		final byte[] encodedBytes = cipher.encrypt(decodedBytes);

		final SecureMessage message = SecureMessage.fromEncodedPayload(
				new Account(senderKeyPair),
				new Account(recipientKeyPair),
				encodedBytes);

		// Assert:
		Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(decodedBytes));
		Assert.assertThat(message.getEncodedPayload(), IsEqual.equalTo(encodedBytes));
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

	//endregion

	//region serialization

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

		final JsonSerializer serializer = new JsonSerializer();
		originalMessage.serialize(serializer);
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Act:
		final byte[] payload = deserializer.readBytes("payload");

		// Assert:
		Assert.assertThat(payload, IsNot.not(IsEqual.equalTo(input)));
	}

	//endregion

	//region private key requirement

	@Test
	public void secureMessageCanBeDecodedWithOnlyRecipientPrivateKey() {
		// Act:
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final SecureMessage message = createRoundTrippedMessage(input, false, true);

		// Assert:
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
	}

	@Test
	public void secureMessageCanBeDecodedWithOnlySenderPrivateKey() {
		// Act:
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final SecureMessage message = createRoundTrippedMessage(input, true, false);

		// Assert:
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
	}

	@Test
	public void secureMessageCannotBeDecodedWithNeitherSenderNorRecipientPrivateKey() {
		// Act:
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final SecureMessage message = createRoundTrippedMessage(input, false, false);

		// Assert:
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(false));
		Assert.assertThat(message.getDecodedPayload(), IsNull.nullValue());
		Assert.assertThat(message.getEncodedPayload(), IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void secureMessageCannotBeDecodedWithNullSenderKeyPair() {
		// Arrange:
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account senderWithoutKeyPair = new Account(Address.fromEncoded(sender.getAddress().getEncoded()));
		final SecureMessage originalMessage = SecureMessage.fromDecodedPayload(sender, recipient, input);

		// Act:
		final SecureMessage message = createRoundTrippedMessage(originalMessage, senderWithoutKeyPair, recipient);

		// Assert:
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(false));
		Assert.assertThat(message.getDecodedPayload(), IsNull.nullValue());
		Assert.assertThat(message.getEncodedPayload(), IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void secureMessageCannotBeDecodedWithNullRecipientKeyPair() {
		// Arrange:
		final byte[] input = new byte[] { 12, 46, 7, 43, 22, 15 };
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final Account recipientWithoutKeyPair = new Account(Address.fromEncoded(recipient.getAddress().getEncoded()));
		final SecureMessage originalMessage = SecureMessage.fromDecodedPayload(sender, recipient, input);

		// Act:
		final SecureMessage message = createRoundTrippedMessage(originalMessage, sender, recipientWithoutKeyPair);

		// Assert:
		Assert.assertThat(message.canDecode(), IsEqual.equalTo(false));
		Assert.assertThat(message.getDecodedPayload(), IsNull.nullValue());
		Assert.assertThat(message.getEncodedPayload(), IsNot.not(IsEqual.equalTo(input)));
	}

	private static SecureMessage createRoundTrippedMessage(
			final byte[] input,
			final boolean useSenderPrivateKey,
			final boolean useRecipientPrivateKey) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final SecureMessage originalMessage = SecureMessage.fromDecodedPayload(sender, recipient, input);

		// Act:
		final Account senderPublicKeyOnly = Utils.createPublicOnlyKeyAccount(sender);
		final Account recipientPublicKeyOnly = Utils.createPublicOnlyKeyAccount(recipient);
		return createRoundTrippedMessage(
				originalMessage,
				useSenderPrivateKey ? sender : senderPublicKeyOnly,
				useRecipientPrivateKey ? recipient : recipientPublicKeyOnly);
	}

	//endregion

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
		return new SecureMessage(deserializer, sender, recipient);
	}
}