package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

// TODO: comment

public class HarvestInfo implements SerializableEntity {

	private final Hash blockHash;
	private final BlockHeight height;
	private final TimeInstant timestamp;
	private final Amount totalFee;

	public HarvestInfo(final Deserializer deserializer) {
		this.blockHash = deserializer.readObject("blockHash", Hash.DESERIALIZER);
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.timestamp = TimeInstant.readFrom(deserializer, "timestamp");
		this.totalFee = Amount.readFrom(deserializer, "totalFee");
	}

	public HarvestInfo(final Hash blockHash, final BlockHeight height, final TimeInstant timestamp, final Amount totalFee) {
		this.blockHash = blockHash;
		this.height = height;
		this.timestamp = timestamp;
		this.totalFee = totalFee;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("blockHash", this.blockHash);
		BlockHeight.writeTo(serializer, "height", this.height);
		TimeInstant.writeTo(serializer, "timestamp", this.getTimestamp());
		Amount.writeTo(serializer, "totalFee", this.totalFee);
	}

	public Hash getHash() {
		return this.blockHash;
	}

	public BlockHeight getBlockHeight() {
		return this.height;
	}

	public TimeInstant getTimestamp() {
		return this.timestamp;
	}

	public Amount getTotalFee() {
		return this.totalFee;
	}
}
