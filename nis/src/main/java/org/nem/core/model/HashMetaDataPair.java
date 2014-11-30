package org.nem.core.model;

import org.nem.core.crypto.Hash;

/**
 * Pair consisting of a hash and a timestamp.
 */
public class HashMetaDataPair {
	private final Hash hash;
	private final HashMetaData metaData;

	public HashMetaDataPair(final Hash hash, final HashMetaData metaData) {
		this.hash = hash;
		this.metaData = metaData;
	}

	/**
	 * Gets the underlying hash.
	 *
	 * @return The hash.
	 */
	public Hash getHash() {
		return this.hash;
	}

	/**
	 * Gets the underlying meta data.
	 *
	 * @return The meta data.
	 */
	public HashMetaData getMetaData() {
		return this.metaData;
	}
}
