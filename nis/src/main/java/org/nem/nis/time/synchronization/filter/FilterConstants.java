package org.nem.nis.time.synchronization.filter;

/**
 * Constants used by the synchronization filter classes.
 */
public class FilterConstants {
	private static final long MINUTE = 60L * 1000L;

	/**
	 * Start value for the maximum tolerated deviation in ms.
	 */
	public static final long TOLERATED_DEVIATION_START = 120 * MINUTE;

	/**
	 * Minimum value for the maximum tolerated deviation in ms.
	 */
	public static final long TOLERATED_DEVIATION_MINIMUM = MINUTE;

	/**
	 * Value that indicates after which round the decay starts.
	 */
	public static final long START_DECAY_AFTER_ROUND = 5;

	/**
	 * Value that indicates how fast the decay is.
	 */
	public static final double DECAY_STRENGTH = 0.3;

	/**
	 * Value that indicates which percentage of the samples is discarded.
	 */
	public static final double ALPHA = 0.4;

	/**
	 * The maximum time in ms that a response to a time sync request may take in ms.
	 */
	public static final long TOLERATED_DURATION_MAXIMUM = 1000;
}
