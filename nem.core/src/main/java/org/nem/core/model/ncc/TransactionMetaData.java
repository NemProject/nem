package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;

/**
 * Class for holding additional information about transaction required by ncc.
 */
public class TransactionMetaData implements SerializableEntity {
	private final BlockHeight height;
	private final Long id;
	private final Hash hash;
	private final Hash innerHash;

	/**
	 * Creates a new meta data.
	 *
	 * @param blockHeight The block height.
	 * @param id The transaction id.
	 * @param hash The transaction hash.
	 */
	public TransactionMetaData(
			final BlockHeight blockHeight,
			final Long id,
			final Hash hash) {
		this(blockHeight, id, hash, null);
	}

	/**
	 * Creates a new meta data.
	 *
	 * @param blockHeight The block height.
	 * @param id The transaction id.
	 * @param hash The transaction hash.
	 * @param innerHash The hash of an inner transaction or null.
	 */
	public TransactionMetaData(
			final BlockHeight blockHeight,
			final Long id,
			final Hash hash,
			final Hash innerHash) {
		this.height = blockHeight;
		this.id = id;
		this.hash = hash;
		this.innerHash = innerHash;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public TransactionMetaData(final Deserializer deserializer) {
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.id = deserializer.readLong("id");
		this.hash = deserializer.readObject("hash", Hash::new);
		this.innerHash = deserializer.readOptionalObject("innerHash", Hash::new);
	}

	/**
	 * Returns height of a transaction.
	 *
	 * @return The height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Returns the id of a transaction.
	 *
	 * @return The id.
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Returns the hash of a transaction.
	 *
	 * @return The hash.
	 */
	public Hash getHash() {
		return this.hash;
	}

	/**
	 * Returns the hash of an inner transaction.
	 *
	 * @return The hash.
	 */
	public Hash getInnerHash() {
		return innerHash;
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		serializer.writeLong("id", this.id);
		serializer.writeObject("hash", this.hash);
		serializer.writeObject("innerHash", this.innerHash);
	}
}
