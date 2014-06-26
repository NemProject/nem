package org.nem.core.model.ncc;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * Pair containing a Block and a BlockMetaData
 */
public class BlockMetaDataPair implements SerializableEntity {
	private final Block block;
	private final BlockMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param block The block.
	 * @param metaData The meta data.
	 */
	public BlockMetaDataPair(final Block block, final BlockMetaData metaData) {
		this.block = block;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public BlockMetaDataPair(final Deserializer deserializer) {
		this.block = deserializer.readObject("block", BlockFactory.VERIFIABLE);
		this.metaData = deserializer.readObject("meta", BlockMetaData.DESERIALIZER);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("block", this.block);
		serializer.writeObject("meta", this.metaData);
	}

	/**
	 * Returns the block.
	 *
	 * @return The block.
	 */
	public Block getBlock() {
		return this.block;
	}

	/**
	 * Returns the meta data.
	 *
	 * @return The meta data.
	 */
	public BlockMetaData getMetaData() {
		return this.metaData;
	}
}
