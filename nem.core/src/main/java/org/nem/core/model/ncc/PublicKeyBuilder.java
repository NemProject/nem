package org.nem.core.model.ncc;

import org.nem.core.crypto.PublicKey;

/**
 * Builder that is used by Spring to create a PublicKey from a GET request.
 */
public class PublicKeyBuilder {
	private String publicKey;

	/**
	 * Sets the public key.
	 *
	 * @param publicKey The public key.
	 */
	public void setPublicKey(final String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an public key.
	 *
	 * @return The public key.
	 */
	public PublicKey build() {
		return PublicKey.fromHexString(this.publicKey);
	}
}