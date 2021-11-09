package org.nem.nis.state;

/**
 * Constants related to weighted balance decays.
 */
public class WeightedBalanceDecayConstants {

	/**
	 * The weighted balance decay numerator.
	 */
	public static final long DECAY_NUMERATOR = 9;

	/**
	 * The weighted balance decay denominator.
	 */
	public static final long DECAY_DENOMINATOR = 10;

	/**
	 * The weighted balance decay rate.
	 */
	public static final double DECAY_BASE = (double) DECAY_NUMERATOR / (double) DECAY_DENOMINATOR;
}
