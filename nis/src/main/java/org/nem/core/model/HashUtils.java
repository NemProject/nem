package org.nem.core.model;

import org.nem.core.crypto.Hashes;
import org.nem.core.crypto.PublicKey;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.utils.ArrayUtils;

/**
 * Static class that provides hashing utilities.
 */
public abstract class HashUtils {

	/**
	 * Calculates the hash of the specified entity, excluding its signature.
	 *
	 * @param entity The entity.
	 *
	 * @return The calculated hash.
	 */
	public static Hash calculateHash(final VerifiableEntity entity) {
		byte[] data = BinarySerializer.serializeToBytes(entity.asNonVerifiable());
		return new Hash(Hashes.sha3(data));
	}

	public static Hash nextHash(final Hash hash, final PublicKey publicKey) {
		return new Hash(Hashes.sha3(ArrayUtils.concat(hash.getRaw(), publicKey.getRaw())));
	}
}
