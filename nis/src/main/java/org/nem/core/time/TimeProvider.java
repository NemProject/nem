package org.nem.core.time;

import org.nem.core.model.primitive.NetworkTimeStamp;
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
}
