package org.nem.peer.node;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.utils.ArrayUtils;
import org.nem.core.utils.ByteUtils;

/**
 * Represents a node identity that uniquely identifies a node.
 */
public class NodeIdentity {

	private final KeyPair keyPair;
	private final Address address;

	/**
	 * Creates a new node identity.
	 *
	 * @param keyPair The keyPair that identifies the node.
	 */
	public NodeIdentity(final KeyPair keyPair) {
		this.keyPair = keyPair;
		this.address = Address.fromPublicKey(this.keyPair.getPublicKey());
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
		return signer.sign(getPayload(salt));
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
		return ArrayUtils.concat(this.address.getPublicKey().getRaw(), salt);
	}

	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof NodeIdentity))
			return false;

		final NodeIdentity rhs = (NodeIdentity)obj;
		return this.address.equals(rhs.address);
	}

	@Override
	public String toString() {
		return this.address.toString();
	}
}
