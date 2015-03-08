package org.nem.core.model.ncc;

import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * Contains information about a harvested block.
 */
public class HarvestInfo implements SerializableEntity {
	private final Long blockId;
	private final BlockHeight height;
	private final TimeInstant timeStamp;
	private final Amount totalFee;
	private final Long difficulty;

	/**
	 * Creates a new harvest info.
	 *
	 * @param blockId The block id.
	 * @param height The height.
	 * @param timeStamp The block timestamp.
	 * @param totalFee The total fee.
	 * @param difficulty The difficulty.
	 */
	public HarvestInfo(
			final Long blockId,
			final BlockHeight height,
			final TimeInstant timeStamp,
			final Amount totalFee,
			final Long difficulty) {
		this.blockId = blockId;
		this.height = height;
		this.timeStamp = timeStamp;
		this.totalFee = totalFee;
		this.difficulty = difficulty;
	}

	/**
	 * Deserializes a harvest info.
	 *
	 * @param deserializer The deserializer.
	 */
	public HarvestInfo(final Deserializer deserializer) {
		this.blockId = deserializer.readLong("id");
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.timeStamp = TimeInstant.readFrom(deserializer, "timeStamp");
		this.totalFee = Amount.readFrom(deserializer, "totalFee");
		this.difficulty = deserializer.readLong("difficulty");
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("id", this.blockId);
		BlockHeight.writeTo(serializer, "height", this.height);
		TimeInstant.writeTo(serializer, "timeStamp", this.getTimeStamp());
		Amount.writeTo(serializer, "totalFee", this.totalFee);
		serializer.writeLong("difficulty", this.difficulty);
	}

	/**
	 * Gets the block id.
	 *
	 * @return The block id.
	 */
	public Long getId() {
		return this.blockId;
	}

	/**
	 * Gets the block height.
	 *
	 * @return The block height.
	 */
	public BlockHeight getBlockHeight() {
		return this.height;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return The timestamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * Gets the total fee.
	 *
	 * @return the total fee.
	 */
	public Amount getTotalFee() {
		return this.totalFee;
	}

	/**
	 * Gets the block difficulty.
	 *
	 * @return The block difficulty.
	 */
	public Long getDifficulty() {
		return this.difficulty;
	}
}
