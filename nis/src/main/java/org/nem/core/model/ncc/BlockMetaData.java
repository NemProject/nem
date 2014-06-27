package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.serialization.*;

/**
 * Metadata about a block.
 */
public class BlockMetaData implements SerializableEntity {
	public static final ObjectDeserializer<BlockMetaData> DESERIALIZER = deserializer -> new BlockMetaData(deserializer);

	private final Hash hash;

	/**
	 * Creates a new meta data.
	 *
	 * @param blockHash The block hash.
	 */
	public BlockMetaData(final Hash blockHash) {
		this.hash = blockHash;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockMetaData(final Deserializer deserializer) {
		this(deserializer.readObject("hash", Hash.DESERIALIZER));
	}

	/**
	 * Returns hash of a transaction.
	 *
	 * @return The hash.
	 */
	public Hash getHash() {
		return this.hash;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("hash", this.getHash());
	}
}