package org.nem.nis.time.synchronization.filter;

/**
 * Constants used by the synchronization filter classes.
 */
public class FilterConstants {

	/**
	 * Start value for the maximum tolerated deviation in ms.
	 */
	public static final long TOLERATED_DEVIATION_START = 300000;

	/**
	 * Minimum value for the maximum tolerated deviation in ms.
	 */
	public static final long TOLERATED_DEVIATION_MINIMUM = 60000;

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
	public static final double ALPHA = 0.3;
}
