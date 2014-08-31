package org.nem.nis.time.synchronization;

import org.nem.core.node.Node;

import java.util.concurrent.CompletableFuture;

/**
 * Interface that is used to sync time across nodes.
 */
public interface TimeSyncConnector {

	/**
	 * Requests information about the network time from the specified node.
	 *
	 * @param node The remote node.
	 * @return The completable future containing the communication time stamps.
	 */
	public CompletableFuture<CommunicationTimeStamps> getCommunicationTimeStamps(final Node node);
}
