package org.nem.core.model;

import org.nem.core.serialization.*;

import java.util.*;

/**
 * Helper class for storing list of hashes. Wraps List of ByteArray objects.
 */
public class HashChain extends SerializableList<Hash> {

	/**
	 * Creates new empty HashChain with specified capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public HashChain(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates new HashChain and initializes it with passed list of hashes.
	 *
	 * @param hashList The list of hashes.
	 */
	public HashChain(final List<Hash> hashList) {
		super(hashList);
	}

	/**
	 * Deserializes a HashChain.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public HashChain(Deserializer deserializer) {
		super(deserializer.readObjectArray("data", Hash.DESERIALIZER));
	}

	/**
	 * Creates new HashChain and initializes it with the passed list of raw hashes.
	 *
	 * @param rawHashList The list of raw hashes.
	 */
	public static HashChain fromRawHashes(final List<byte[]> rawHashList) {
		final List<Hash> hashList = new ArrayList<>(rawHashList.size());
		for (final byte[] rawHash : rawHashList) {
			hashList.add(new Hash(rawHash));
		}

		return new HashChain(hashList);
	}
}
