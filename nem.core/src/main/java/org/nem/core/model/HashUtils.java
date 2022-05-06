package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

/**
 * Static class that provides hashing utilities.
 */
public abstract class HashUtils {

	/**
	 * Calculates the hash of the specified entity, excluding its signature.
	 *
	 * @param entity The entity.
	 * @return The calculated hash.
	 */
	public static Hash calculateHash(final VerifiableEntity entity) {
		return calculateHash(entity.asNonVerifiable());
	}

	/**
	 * Calculates the hash of the specified entity.
	 *
	 * @param entity The entity.
	 * @return The calculated hash.
	 */
	public static Hash calculateHash(final SerializableEntity entity) {
		final byte[] data = BinarySerializer.serializeToBytes(entity);
		return new Hash(Hashes.sha3_256(data));
	}

	/**
	 * Calculates the hash of the concatenated entities.
	 *
	 * @param hash The first object.
	 * @param publicKey The second object.
	 * @return The hash.
	 */
	public static Hash nextHash(final Hash hash, final PublicKey publicKey) {
		return new Hash(Hashes.sha3_256(hash.getRaw(), publicKey.getRaw()));
	}
}
