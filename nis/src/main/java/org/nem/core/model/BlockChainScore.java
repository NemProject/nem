package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class BlockChainScore extends AbstractPrimitive<BlockChainScore> implements SerializableEntity {
	/**
	 * Value representing initial score.
	 */
	public static final BlockChainScore ZERO = new BlockChainScore(0);

	/**
	 * Creates a block chain score.
	 *
	 * @param score The block chain score.
	 */
	public BlockChainScore(final long score) {
		super(score, BlockChainScore.class);

		if (this.getRaw() < 0)
			throw new IllegalArgumentException("block chain score can't be negative");
	}

	/**
	 * Deserializes a chain score.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockChainScore(final Deserializer deserializer) {
		this(deserializer.readLong("score"));
	}

	/**
	 * Returns the underlying score.
	 *
	 * @return The underlying score.
	 */
	public long getRaw() { 
		return this.getValue(); 
	}
	
	/**
	 * Creates a new BlockChainScore by adding the given value to this score.
	 *
	 * @param value the value to add.
	 * @return The new score.
	 */
	public BlockChainScore add(final BlockChainScore score) {
		return new BlockChainScore(this.getRaw() + score.getRaw());
	}
	
	/**
	 * Creates a new BlockChainScore by adding the given value to this score.
	 *
	 * @param value the value to add.
	 * @return The new score.
	 */
	public BlockChainScore subtract(final BlockChainScore score) {
		return new BlockChainScore(this.getRaw() - score.getRaw());
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("score", this.getRaw());
	}
}
