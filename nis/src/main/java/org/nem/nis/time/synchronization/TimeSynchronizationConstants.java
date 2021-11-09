package org.nem.nis.time.synchronization;

/**
 * Constants used by the time synchronization classes.
 */
public class TimeSynchronizationConstants {

	/**
	 * Start value for the coupling of clocks.
	 */
	public static final double COUPLING_START = 1.0;

	/**
	 * The minimal value for the coupling of clocks.
	 */
	public static final double COUPLING_MINIMUM = 0.1;

	/**
	 * Value that indicates after which round the decay starts.
	 */
	public static final long START_COUPLING_DECAY_AFTER_ROUND = 5;

	/**
	 * Value that indicates how fast the coupling decay is.
	 */
	public static final double COUPLING_DECAY_STRENGTH = 0.3;

	/**
	 * Value that indicates how large the change in network time must be in order to update the node's network time. This constant is used
	 * to prevent slow shifts in network time. The unit of this constant is milli seconds.
	 */
	public static final long CLOCK_ADJUSTMENT_THRESHOLD = 75;

	/**
	 * The minimum importance a node must have in order to be considered as synchronization partner. The value corresponds to having a
	 * vested balance of 1 NEM stake.
	 */
	public static final double REQUIRED_MINIMUM_IMPORTANCE = 0.00025;
}
