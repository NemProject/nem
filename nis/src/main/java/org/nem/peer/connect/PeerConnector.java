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
	 * @param endpoint The endpoint.
	 *
	 * @return Information about the specified node.
	 */
	public CompletableFuture<Node> getInfo(final NodeEndpoint endpoint);

	/**
	 * Requests information about all known peers from the specified node.
	 *
	 * @param endpoint The endpoint.
	 *
	 * @return A collection of all known peers.
	 */
	public CompletableFuture<NodeCollection> getKnownPeers(final NodeEndpoint endpoint);

	/**
	 * Announces a new entity to the target node.
	 *
	 * @param endpoint   The endpoint.
	 * @param announceId The type of announcement.
	 * @param entity     The entity to announce.
	 */
	public CompletableFuture announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity);
}