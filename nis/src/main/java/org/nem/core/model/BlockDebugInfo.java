package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;
import org.nem.core.time.TimeInstant;

public class BlockDebugInfo  implements SerializableEntity {

	private BlockHeight height;
	private Address foragerAddress;
	private final TimeInstant timestamp;
	private BlockDifficulty difficulty;

	/**
	 * Creates a new block debug info.
	 *
	 * @param blockHeight The block height.
	 */
	public BlockDebugInfo(final BlockHeight blockHeight, final Address foragerAddress, final TimeInstant timestamp, BlockDifficulty difficulty) {
		this.height = blockHeight;
		this.foragerAddress = foragerAddress;
		this.timestamp = timestamp;
		this.difficulty = difficulty;
	}
	
	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockDebugInfo(final Deserializer deserializer) {
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.foragerAddress = Address.readFrom(deserializer, "foragerAddress");
		this.timestamp = TimeInstant.readFrom(deserializer, "timestamp");
		this.difficulty = BlockDifficulty.readFrom(deserializer, "difficulty");
	}

	@Override
	public void serialize(Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		Address.writeTo(serializer, "forager", foragerAddress);
		TimeInstant.writeTo(serializer, "timestamp", this.timestamp);
		BlockDifficulty.writeTo(serializer, "difficulty", this.difficulty);
	}

}
