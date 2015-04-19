package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

import java.math.BigInteger;

/**
 * Represents a NEM block difficulty.
 */
public class BlockDifficulty extends AbstractPrimitive<BlockDifficulty, Long> {

	/**
	 * The initial block difficulty.
	 * 1_500_000_000 NEMs have the force to generate block every minute with initial difficulty.
	 * This means that if the total number of coins used to harvest drops below 150_000_000
	 * then the average block times will be higher than 1 minute.
	 * 150_000_000 is approximately 1.67% of all coins.
	 */
	public static final BlockDifficulty INITIAL_DIFFICULTY = new BlockDifficulty(100_000_000_000_000L, false);

	/**
	 * Minimum value for difficulty.
	 */
	private static final long MIN_DIFFICULTY = INITIAL_DIFFICULTY.getRaw() / 10L;

	/**
	 * Maximum value for difficulty.
	 */
	private static final long MAX_DIFFICULTY = INITIAL_DIFFICULTY.getRaw() * 10L;

	/**
	 * Creates a block difficulty and automatically clamps the raw value.
	 *
	 * @param difficulty The block difficulty.
	 */
	public BlockDifficulty(final long difficulty) {
		this(difficulty, true);
	}

	private BlockDifficulty(final long difficulty, final boolean clamp) {
		super(clamp ? Clamp(difficulty) : difficulty, BlockDifficulty.class);
	}

	/**
	 * Returns the underlying difficulty.
	 *
	 * @return The underlying difficulty.
	 */
	public long getRaw() {
		return this.getValue();
	}

	/**
	 * Returns the underlying difficulty as a BigInteger.
	 *
	 * @return The underlying difficulty as a BigInteger.
	 */
	public BigInteger asBigInteger() {
		return BigInteger.valueOf(this.getValue());
	}

	private static long Clamp(final long difficulty) {
		return Math.min(MAX_DIFFICULTY, Math.max(MIN_DIFFICULTY, difficulty));
	}

	//region inline serialization

	/**
	 * Writes a block difficulty object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param difficulty The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final BlockDifficulty difficulty) {
		serializer.writeLong(label, difficulty.getRaw());
	}

	/**
	 * Reads a block difficulty object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static BlockDifficulty readFrom(final Deserializer deserializer, final String label) {
		return new BlockDifficulty(deserializer.readLong(label));
	}

	//endregion
}
