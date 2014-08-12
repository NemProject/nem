package org.nem.core.model.ncc;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;

/**
 * Class for holding additional information about transaction required by ncc.
 */
public class TransactionMetaData implements SerializableEntity {

	private final BlockHeight height;

	/**
	 * Creates a new meta data.
	 *
	 * @param blockHeight The block height.
	 */
	public TransactionMetaData(final BlockHeight blockHeight) {
		this.height = blockHeight;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public TransactionMetaData(final Deserializer deserializer) {
		this(BlockHeight.readFrom(deserializer, "height"));
	}

	/**
	 * Returns height of a transaction.
	 *
	 * @return The height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
	}
}
