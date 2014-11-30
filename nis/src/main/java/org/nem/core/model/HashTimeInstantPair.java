package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.time.TimeInstant;

/**
 * Pair consisting of a hash and a timestamp.
 */
public class HashTimeInstantPair {
	private final Hash hash;
	private final TimeInstant timeStamp;

	public HashTimeInstantPair(final Hash hash, final TimeInstant timeStamp) {
		this.hash = hash;
		this.timeStamp = timeStamp;
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
	 * Gets the underlying timestamp.
	 *
	 * @return The timestamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timeStamp;
	}
}
