package org.nem.core.model.ncc;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;

/**
 * Class for holding additional information about transaction required by ncc.
 */
public class TransactionMetaData implements SerializableEntity {

	private final BlockHeight height;
	private final Long id;

	/**
	 * Creates a new meta data.
	 *
	 * @param blockHeight The block height.
	 * @param id The transaction id.
	 */
	public TransactionMetaData(final BlockHeight blockHeight, final Long id) {
		this.height = blockHeight;
		this.id = id;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public TransactionMetaData(final Deserializer deserializer) {
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.id = deserializer.readLong("id");
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
	 * Returns height of a transaction.
	 *
	 * @return The height.
	 */
	public Long getId() {
		return this.id;
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		serializer.writeLong("id", this.id);
	}
}
