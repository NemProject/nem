package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.serialization.*;

/**
 * Class for holding additional information about an unconfirmed transaction required by clients.
 * The inner transaction hash is allowed to be null.
 */
public class UnconfirmedTransactionMetaData implements SerializableEntity {

	private final Hash innerTransactionHash;

	/**
	 * Creates a new meta data.
	 *
	 * @param innerTransactionHash The hash of the optional inner transaction.
	 */
	public UnconfirmedTransactionMetaData(final Hash innerTransactionHash) {
		this.innerTransactionHash = innerTransactionHash;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public UnconfirmedTransactionMetaData(final Deserializer deserializer) {
		final byte[] rawHash = deserializer.readOptionalBytes("data");
		this.innerTransactionHash = null == rawHash ? null : new Hash(rawHash);
	}

	/**
	 * Returns the hash of the optional inner transaction.
	 *
	 * @return The height.
	 */
	public Hash getInnerTransactionHash() {
		return this.innerTransactionHash;
	}

	@Override
	public void serialize(final Serializer serializer) {
		if (null != this.innerTransactionHash) {
			this.innerTransactionHash.serialize(serializer);
		} else {
			serializer.writeBytes("data", null);
		}
	}
}
