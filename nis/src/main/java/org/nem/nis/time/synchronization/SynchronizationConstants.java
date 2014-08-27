package org.nem.nis.time.synchronization;

/**
 * Constants used by the time synchronization classes.
 */
public class SynchronizationConstants {

	/***
	 * Start value for the coupling of clocks.
	 */
	public static final double COUPLING_START = 1.0;

	/***
	 * The minimal value for the coupling of clocks.
	 */
	public static final double COUPLING_MINIMUM = 0.1;

	/***
	 * Value that indicates after which round the decay starts.
	 */
	public static final long START_COUPLING_DECAY_AFTER_ROUND = 5;

	/***
	 * Value that indicates how fast the coupling decay is.
	 */
	public static final double COUPLING_DECAY_STRENGTH = 0.3;

	/***
	 * Start value for the update interval of clocks in milli seconds.
	 */
	public static final long UPDATE_INTERVAL_START = 60000;

	/***
	 * The maximal value for the update interval of clocks in milli seconds.
	 */
	public static final long UPDATE_INTERVAL_MAXIMUM = 60 * UPDATE_INTERVAL_START;

	/***
	 * Value that indicates after which round the update interval elongation starts.
	 */
	public static final long START_UPDATE_INTERVAL_ELONGATION_AFTER_ROUND = 5;

	/***
	 * Value that indicates how fast the update interval grows.
	 */
	public static final double UPDATE_INTERVAL_ELONGATION_STRENGTH = 0.3;

	/***
	 * Value that indicates how large the change in network time must be
	 * in order to update the node's network time.
	 * This constant is used to prevent slow shifts in network time.
	 * The unit of this constant is milli seconds.
	 */
	public static final long CLOCK_ADJUSTMENT_THRESHOLD = 50;
}
