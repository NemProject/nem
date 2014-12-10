package org.nem.core.messages;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.util.Arrays;

/**
 * A secure, encrypted message.
 */
public class SecureMessage extends Message {

	private final SecureMessagePayload payload;

	private SecureMessage(final Account sender, final Account recipient, final byte[] payload) {
		super(MessageTypes.SECURE);
		this.payload = new AccountBasedSecureMessagePayload(sender, recipient, payload);
	}

	/**
	 * Creates a new secure message around a decoded payload that should be encrypted.
	 *
	 * @param sender The message sender.
	 * @param recipient The message recipient.
	 * @param payload The unencrypted payload.
	 */
	public static SecureMessage fromDecodedPayload(final Account sender, final Account recipient, final byte[] payload) {
		if (!sender.hasPrivateKey()) {
			throw new IllegalArgumentException("sender private key is required for creating secure message");
		}

		final Cipher cipher = sender.createCipher(recipient, true);
		return new SecureMessage(sender, recipient, cipher.encrypt(payload));
	}

	/**
	 * Creates a new secure message around an encoded payload that is already encrypted.
	 *
	 * @param sender The message sender.
	 * @param recipient The message recipient.
	 * @param payload The encrypted payload.
	 */
	public static SecureMessage fromEncodedPayload(final Account sender, final Account recipient, final byte[] payload) {
		return new SecureMessage(sender, recipient, payload);
	}

	/**
	 * Deserializes a secure message.
	 *
	 * @param deserializer The deserializer.
	 */
	public SecureMessage(final Deserializer deserializer) {
		super(MessageTypes.SECURE);
		final byte[] payload = deserializer.readBytes("payload");
		final Deserializer payloadDeserializer = new BinaryDeserializer(payload, deserializer.getContext());
		this.payload = new DeserializedSecureMessagePayload(payloadDeserializer);
	}

	@Override
	public boolean canDecode() {
		return this.payload.canDecode();
	}

	@Override
	public byte[] getEncodedPayload() {
		return this.payload.getEncoded();
	}

	@Override
	public byte[] getDecodedPayload() {
		return this.payload.getDecoded();
	}

	@Override
	public void serialize(final Serializer serializer) {
		super.serialize(serializer);
		serializer.writeBytes("payload", BinarySerializer.serializeToBytes(this.payload));
	}

	@Override
	public int hashCode() {
		return this.payload.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SecureMessage)) {
			return false;
		}

		final SecureMessage rhs = (SecureMessage)obj;
		return this.payload.equals(rhs.payload);
	}

	//region SecureMessagePayload

	private static abstract class SecureMessagePayload implements SerializableEntity {
		private final Address senderAddress;
		private final Address recipientAddress;
		private final byte[] payload;

		protected SecureMessagePayload(final Address senderAddress, final Address recipientAddress, final byte[] payload) {
			this.senderAddress = senderAddress;
			this.recipientAddress = recipientAddress;
			this.payload = payload;
		}

		protected SecureMessagePayload(final Deserializer deserializer) {
			this.senderAddress = Address.readFrom(deserializer, "sender");
			this.recipientAddress = Address.readFrom(deserializer, "recipient");
			this.payload = deserializer.readBytes("payload");
		}

		public boolean canDecode() {
			return this.getSender().hasPrivateKey() && this.getRecipient().hasPublicKey()
					|| this.getRecipient().hasPrivateKey() && this.getSender().hasPublicKey();
		}

		public byte[] getEncoded() {
			return this.payload;
		}

		public byte[] getDecoded() {
			if (!this.canDecode()) {
				return null;
			}

			final Cipher cipher = this.getSender().createCipher(this.getRecipient(), false);
			return cipher.decrypt(this.payload);
		}

		@Override
		public void serialize(final Serializer serializer) {
			Address.writeTo(serializer, "sender", this.senderAddress);
			Address.writeTo(serializer, "recipient", this.recipientAddress);
			serializer.writeBytes("payload", this.payload);
		}

		protected Address getSenderAddress() {
			return this.senderAddress;
		}

		protected Address getRecipientAddress() {
			return this.recipientAddress;
		}

		protected abstract Account getSender();

		protected abstract Account getRecipient();

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.payload);
		}

		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof SecureMessagePayload)) {
				return false;
			}

			final SecureMessagePayload rhs = (SecureMessagePayload)obj;
			return Arrays.equals(this.payload, rhs.payload)
					&& this.senderAddress.equals(rhs.senderAddress)
					&& this.recipientAddress.equals(rhs.recipientAddress);
		}
	}

	private static class DeserializedSecureMessagePayload extends SecureMessagePayload {
		private final DeserializationContext deserializationContext;

		public DeserializedSecureMessagePayload(final Deserializer deserializer) {
			super(deserializer);
			this.deserializationContext = deserializer.getContext();
		}

		@Override
		protected Account getSender() {
			return this.deserializationContext.findAccountByAddress(this.getSenderAddress());
		}

		@Override
		protected Account getRecipient() {
			return this.deserializationContext.findAccountByAddress(this.getRecipientAddress());
		}
	}

	private static class AccountBasedSecureMessagePayload extends SecureMessagePayload {
		private final Account sender;
		private final Account recipient;

		public AccountBasedSecureMessagePayload(final Account sender, final Account recipient, final byte[] payload) {
			super(sender.getAddress(), recipient.getAddress(), payload);
			this.sender = sender;
			this.recipient = recipient;
		}

		@Override
		protected Account getSender() {
			return this.sender;
		}

		@Override
		protected Account getRecipient() {
			return this.recipient;
		}
	}

	//endregion
}

