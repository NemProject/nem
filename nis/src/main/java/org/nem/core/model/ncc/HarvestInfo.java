package org.nem.core.model.ncc;

import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * Contains information about a harvested block.
 */
public class HarvestInfo implements SerializableEntity {
	public static ObjectDeserializer<HarvestInfo> DESERIALIZER = HarvestInfo::new;

	private final Long blockId;
	private final BlockHeight height;
	private final TimeInstant timeStamp;
	private final Amount totalFee;

	/**
	 * Creates a new harvest info.
	 *
	 * @param blockId The block id.
	 * @param height The height.
	 * @param timeStamp The block timestamp.
	 * @param totalFee The total fee.
	 */
	public HarvestInfo(final Long blockId, final BlockHeight height, final TimeInstant timeStamp, final Amount totalFee) {
		this.blockId = blockId;
		this.height = height;
		this.timeStamp = timeStamp;
		this.totalFee = totalFee;
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
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("id", this.blockId);
		BlockHeight.writeTo(serializer, "height", this.height);
		TimeInstant.writeTo(serializer, "timeStamp", this.getTimeStamp());
		Amount.writeTo(serializer, "totalFee", this.totalFee);
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
}
