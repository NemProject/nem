package org.nem.core.time;

// TODO 20141112 J-B: i think i would prefer to move these dependencies into this (core.time) package
import org.nem.core.model.primitive.*;
import org.nem.nis.controller.viewmodels.TimeSynchronizationResult;

/**
 * Interface that provides time-related information.
 */
public interface TimeProvider {

	/**
	 * Gets the epoch time.
	 *
	 * @return The epoch time.
	 */
	public TimeInstant getEpochTime();

	/**
	 * Gets the current time.
	 *
	 * @return The current time.
	 */
	public TimeInstant getCurrentTime();

	/**
	 * Gets the network time in ms.
	 *
	 * @return The network time stamp.
	 */
	public NetworkTimeStamp getNetworkTime();

	/**
	 * Updates the time offset.
	 *
	 * @param offset The calculated time offset to the other nodes.
	 * @return The time synchronization result.
	 */
	public TimeSynchronizationResult updateTimeOffset(final TimeOffset offset);
}
