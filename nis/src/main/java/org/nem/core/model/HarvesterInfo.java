package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;


public class HarvesterInfo implements SerializableEntity {

	private final Hash blockHash;
	private final BlockHeight height;
	private final Amount totalFee;

	public HarvesterInfo(final Deserializer deserializer) {
		this.blockHash = deserializer.readObject("blockHash", Hash.DESERIALIZER);
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.totalFee = Amount.readFrom(deserializer, "totalFee");
	}

	public HarvesterInfo(final Hash blockHash, final BlockHeight height, final Amount totalFee) {
		this.blockHash = blockHash;
		this.height = height;
		this.totalFee = totalFee;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("blockHash", this.blockHash);
		BlockHeight.writeTo(serializer, "height", this.height);
		Amount.writeTo(serializer, "totalFee", this.totalFee);
	}

	public Hash getHash() {
		return this.blockHash;
	}

	public BlockHeight getBlockHeight() {
		return this.height;
	}

	public Amount getTotalFee() {
		return this.totalFee;
	}
}
