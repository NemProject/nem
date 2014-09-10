package org.nem.core.time;

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

	// TODO 20140909 J-B i'm not sure i like this, but i need to give it a little more thought
	// i guess the reason for networktimestamp is for more fine-grained ms resolution?

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
