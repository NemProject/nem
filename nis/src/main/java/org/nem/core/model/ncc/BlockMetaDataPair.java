package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.BlockFactory;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class BlockMetaDataPair implements SerializableEntity {
	final Block block;
	final BlockMetaData blockMetaData;

	public BlockMetaDataPair(final Block block, final BlockMetaData blockMetaData) {
		this.block = block;
		this.blockMetaData = blockMetaData;
	}

	public BlockMetaDataPair(final Deserializer deserializer) {
		this.block = deserializer.readObject("block", BlockFactory.VERIFIABLE);
		this.blockMetaData = deserializer.readObject("meta", BlockMetaData.DESERIALIZER);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("block", this.block);
		serializer.writeObject("meta", this.blockMetaData);
	}

	public Block getBlock() {
		return this.block;
	}

	public BlockMetaData getBlockMetaData() {
		return this.blockMetaData;
	}
}
