package org.nem.core.model;

import org.nem.core.crypto.Hash;

/**
 * Pair consisting of a hash and metadata.
 */
public class HashMetaDataPair {
	private final Hash hash;
	private final HashMetaData metaData;

	/**
	 * Creates a pair.
	 *
	 * @param hash The hash.
	 * @param metaData The metadata.
	 */
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

	@Override
	public int hashCode() {
		return this.hash.hashCode() ^ this.metaData.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof HashMetaDataPair)) {
			return false;
		}

		final HashMetaDataPair rhs = (HashMetaDataPair)obj;
		return this.hash.equals(rhs.hash)
				&& this.metaData.equals(rhs.metaData);
	}
}
