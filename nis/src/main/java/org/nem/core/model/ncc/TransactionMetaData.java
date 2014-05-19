package org.nem.core.model.ncc;

import org.nem.core.model.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Class for holding additional information about transaction required by ncc.
 */
public class TransactionMetaData implements SerializableEntity {
	private BlockHeight height;

	public TransactionMetaData(final BlockHeight blockHeight) {
		this.height = blockHeight;
	}

	public TransactionMetaData(final Deserializer deserializer) {
		this(BlockHeight.readFrom(deserializer, "height"));
	}

	/**
	 * Returns height of a Transaction.
	 *
	 * @return The height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	@Override
	public void serialize(Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
	}
}
