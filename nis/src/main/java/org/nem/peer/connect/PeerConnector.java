package org.nem.peer.connect;

import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.node.*;

import java.util.concurrent.CompletableFuture;

/**
 * A interface that is used to request information from nodes.
 */
public interface PeerConnector {

	/**
	 * Gets information about the specified node.
	 *
	 * @param endpoint The remote endpoint.
	 *
	 * @return Information about the specified node.
	 */
	public CompletableFuture<Node> getInfo(final NodeEndpoint endpoint);

	/**
	 * Requests information about all known peers from the specified node.
	 *
	 * @param endpoint The remote endpoint.
	 *
	 * @return A collection of all known peers.
	 */
	public CompletableFuture<NodeCollection> getKnownPeers(final NodeEndpoint endpoint);

	/**
	 * Requests information about the local (requesting) node.
	 * Can be used to determine remote IP address and refresh local node information.
	 * This enables a kind of auto-magical configuration.
	 *
	 * @param endpoint The remote endpoint.
	 * @param localEndpoint The local endpoint (what the local node knows about itself).
	 *
	 * @return Information about the requesting node.
	 */
	public CompletableFuture<NodeEndpoint> getLocalNodeInfo(
			final NodeEndpoint endpoint,
			final NodeEndpoint localEndpoint);

	/**
	 * Announces a new entity to the target node.
	 *
	 * @param endpoint   The remote endpoint.
	 * @param announceId The type of announcement.
	 * @param entity     The entity to announce.
	 */
	public CompletableFuture announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity);
}