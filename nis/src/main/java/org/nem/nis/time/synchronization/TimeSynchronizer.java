package org.nem.nis.time.synchronization;

/**
 * Synchronizes a node's network time with other nodes.
 * TODO-CR: J-B are you using this?
 */
public interface TimeSynchronizer {

	/**
	 * Synchronizes a node's network time with other nodes.
	 */
	public void synchronizeTime();
}
