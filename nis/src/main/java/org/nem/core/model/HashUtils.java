package org.nem.core.model;

import org.nem.core.crypto.Hashes;
import org.nem.core.serialization.BinarySerializer;

/**
 * Static class that provides hashing utilities.
 */
public class HashUtils {

	/**
	 * Calculates the hash of the specified entity, excluding its signature.
	 *
	 * @param entity The entity.
	 *
	 * @return The calculated hash.
	 */
	public static byte[] calculateHash(final VerifiableEntity entity) {
		byte[] data = BinarySerializer.serializeToBytes(entity.asNonVerifiable());
		return Hashes.sha3(data);
	}
}
