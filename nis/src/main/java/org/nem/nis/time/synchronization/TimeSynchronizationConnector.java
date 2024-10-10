package org.nem.nis.time.synchronization;

import java.util.concurrent.CompletableFuture;
import org.nem.core.node.Node;
import org.nem.core.time.synchronization.CommunicationTimeStamps;

/**
 * Interface that is used to request network time stamps from other nodes.
 */
public interface TimeSynchronizationConnector {

	/**
	 * Requests network time stamps from another node.
	 *
	 * @param node The node to request the time stamps from.
	 * @return The communication time stamps.
	 */
	CompletableFuture<CommunicationTimeStamps> getCommunicationTimeStamps(final Node node);
}
