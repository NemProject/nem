package org.nem.core.time.synchronization;

/**
 * Synchronizes the network time with other nodes.
 * TODO-CR: J-B are you using this?
 * TODO     BR -> J I will, it's not finished yet.
 */
public interface TimeSynchronizer {

	/**
	 * Synchronizes the network time with other nodes.
	 */
	public void synchronizeTime();
}
