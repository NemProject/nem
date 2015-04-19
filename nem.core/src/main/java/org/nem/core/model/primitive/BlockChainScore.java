package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

import java.math.BigInteger;

/**
 * Represents a score for an entire block chain.
 */
public class BlockChainScore extends AbstractPrimitive<BlockChainScore, BigInteger> implements SerializableEntity {

	/**
	 * Value representing initial score.
	 */
	public static final BlockChainScore ZERO = new BlockChainScore(BigInteger.ZERO);

	/**
	 * Creates a block chain score.
	 *
	 * @param score The block chain score.
	 */
	public BlockChainScore(final BigInteger score) {
		super(score, BlockChainScore.class);

		if (this.getRaw().compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("block chain score can't be negative");
		}
	}

	/**
	 * Creates a block chain score from a given long value.
	 *
	 * @param score The block chain score.
	 */
	public BlockChainScore(final long score) {
		this(BigInteger.valueOf(score));
	}

	/**
	 * Deserializes a chain score.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockChainScore(final Deserializer deserializer) {
		this(deserializer.readBigInteger("score"));
	}

	/**
	 * Returns the underlying score.
	 *
	 * @return The underlying score.
	 */
	public BigInteger getRaw() {
		return this.getValue();
	}

	/**
	 * Creates a new BlockChainScore by adding the given value to this score.
	 *
	 * @param score The value to add.
	 * @return The new score.
	 */
	public BlockChainScore add(final BlockChainScore score) {
		return new BlockChainScore(this.getRaw().add(score.getRaw()));
	}

	/**
	 * Creates a new BlockChainScore by subtracting the given value from this score.
	 *
	 * @param score The value to subtract.
	 * @return The new score.
	 */
	public BlockChainScore subtract(final BlockChainScore score) {
		return new BlockChainScore(this.getRaw().subtract(score.getRaw()));
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBigInteger("score", this.getRaw());
	}
}
