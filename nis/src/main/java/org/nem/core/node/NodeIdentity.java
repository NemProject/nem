package org.nem.core.node;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.utils.*;

/**
 * Represents a node identity that uniquely identifies a node.
 */
public class NodeIdentity implements SerializableEntity {
	private static final byte[] CHALLENGE_PREFIX = StringEncoder.getBytes("nem trust challenge:");
	private final KeyPair keyPair;
	private final Address address;
	private String name;

	/**
	 * Creates a new node identity.
	 *
	 * @param keyPair The keyPair that identifies the node.
	 */
	public NodeIdentity(final KeyPair keyPair) {
		this(keyPair, null);
	}

	/**
	 * Creates a new node identity with an optional friendly-name.
	 *
	 * @param keyPair The keyPair that identifies the node.
	 * @param name The friendly name.
	 */
	public NodeIdentity(final KeyPair keyPair, final String name) {
		this.keyPair = keyPair;
		this.address = Address.fromPublicKey(this.keyPair.getPublicKey());
		this.name = name;
	}

	private NodeIdentity(final Deserializer deserializer, final boolean containsPrivateKey) {
		this.keyPair = deserializeKeyPair(deserializer, containsPrivateKey);
		this.address = Address.fromPublicKey(this.keyPair.getPublicKey());
		this.name = deserializer.readOptionalString("name");
	}

	private static KeyPair deserializeKeyPair(final Deserializer deserializer, final boolean containsPrivateKey) {
		if (containsPrivateKey) {
			return new KeyPair(new PrivateKey(deserializer.readBigInteger("private-key")));
		}

		return new KeyPair(new PublicKey(deserializer.readBytes("public-key")));
	}

	/**
	 * Deserializes a node identity with a private key.
	 *
	 * @param deserializer The deserializer.
	 * @return The node identity.
	 */
	public static NodeIdentity deserializeWithPrivateKey(final Deserializer deserializer) {
		return new NodeIdentity(deserializer, true);
	}

	/**
	 * Deserializes a node identity with a public key.
	 *
	 * @param deserializer The deserializer.
	 * @return The node identity.
	 */
	public static NodeIdentity deserializeWithPublicKey(final Deserializer deserializer) {
		return new NodeIdentity(deserializer, false);
	}

	/**
	 * Gets the key pair associated with this identity.
	 *
	 * @return The key pair associated with this identity.
	 */
	public KeyPair getKeyPair() {
		return this.keyPair;
	}

	/**
	 * Gets the address associated with this identity.
	 *
	 * @return The address associated with this identity.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the friendly name associated with this identity.
	 *
	 * @return The friendly name associated with this identity.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the friendly name associated with this identity.
	 *
	 * @param name The friendly name associated with this identity.
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets a value indicating whether or not this identity is owned.
	 *
	 * @return A value indicating whether or not this identity is owned.
	 */
	public boolean isOwned() {
		return this.keyPair.hasPrivateKey();
	}

	/**
	 * Signs information about this identity with its private key.
	 *
	 * @param salt The salt.
	 * @return The signature.
	 */
	public Signature sign(final byte[] salt) {
		final Signer signer = new Signer(this.keyPair);
		return signer.sign(this.getPayload(salt));
	}

	/**
	 * Verifies the signature originated from this identity.
	 *
	 * @param salt The salt.
	 * @param signature The signature.
	 * @return The signature.
	 */
	public boolean verify(final byte[] salt, final Signature signature) {
		final Signer signer = new Signer(this.keyPair);
		return signer.verify(this.getPayload(salt), signature);
	}

	private byte[] getPayload(final byte[] salt) {
		return ArrayUtils.concat(
				CHALLENGE_PREFIX,
				this.address.getPublicKey().getRaw(),
				salt);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("public-key", this.keyPair.getPublicKey().getRaw());
		serializer.writeString("name", this.name);
	}

	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeIdentity)) {
			return false;
		}

		final NodeIdentity rhs = (NodeIdentity)obj;
		return this.address.equals(rhs.address);
	}

	@Override
	public String toString() {
		if (null == this.name) {
			return String.format("<%s>", this.address);
		}

		return String.format("%s <%s>", this.name, this.address);
	}
}
