package org.nem.core.model;

import java.math.BigInteger;

/**
 * Represents a NEM block height.
 */
public class BlockDifficulty extends AbstractPrimitive<BlockDifficulty> {

	/**
	 * The initial block difficulty.
	 * 1_000_000_000 NEMs have force to generate block every minute.
	 */
	public static final BlockDifficulty INITIAL_DIFFICULTY = new BlockDifficulty(120_000_000_000L, false);

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
	public BlockDifficulty(long difficulty) {
		this(difficulty, true);
	}

	private BlockDifficulty(long difficulty, boolean clamp) {
		super(clamp ? Clamp(difficulty) : difficulty, BlockDifficulty.class);
	}

	/**
	 * Returns the underlying difficulty.
	 *
	 * @return The underlying difficulty.
	 */
	public long getRaw() { return this.getValue(); }

	/**
	 * Returns the underlying difficulty as a BigInteger.
	 *
	 * @return The underlying difficulty as a BigInteger.
	 */
	public BigInteger asBigInteger() {
		return BigInteger.valueOf(this.getValue());
	}

	private static long Clamp(long difficulty) {
		return Math.min(MAX_DIFFICULTY, Math.max(MIN_DIFFICULTY, difficulty));
	}
}
