package org.nem.core.time.synchronization;

import java.util.concurrent.CompletableFuture;

/**
 * Synchronizes the network time with other nodes.
 */
public interface TimeSynchronizer {

	/**
	 * Synchronizes the network time with other nodes.
	 *
	 * @return The future.
	 */
	CompletableFuture<Void> synchronizeTime();
}
