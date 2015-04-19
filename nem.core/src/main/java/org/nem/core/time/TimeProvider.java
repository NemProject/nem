package org.nem.core.time;

import org.nem.core.model.primitive.TimeOffset;

/**
 * Interface that provides time-related information.
 */
public interface TimeProvider {

	/**
	 * Gets the epoch time.
	 *
	 * @return The epoch time.
	 */
	TimeInstant getEpochTime();

	/**
	 * Gets the current time.
	 *
	 * @return The current time.
	 */
	TimeInstant getCurrentTime();

	/**
	 * Gets the network time in ms.
	 *
	 * @return The network time stamp.
	 */
	NetworkTimeStamp getNetworkTime();

	/**
	 * Updates the time offset.
	 *
	 * @param offset The calculated time offset to the other nodes.
	 * @return The time synchronization result.
	 */
	TimeSynchronizationResult updateTimeOffset(final TimeOffset offset);
}
