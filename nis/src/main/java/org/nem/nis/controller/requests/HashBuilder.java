package org.nem.nis.controller.requests;

import org.nem.core.crypto.Hash;

/**
 * Builder that is used by Spring to create a hash from a GET request.
 */
public class HashBuilder {
	private String hash;

	/**
	 * Sets the hash.
	 *
	 * @param hash The hash.
	 */
	public void setHash(final String hash) {
		this.hash = hash;
	}

	/**
	 * Creates a hash.
	 *
	 * @return The hash.
	 */
	public Hash build() {
		return Hash.fromHexString(this.hash);
	}
}
