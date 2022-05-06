package org.nem.peer.services;

import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.connect.PeerConnector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Helper class used to implement node broadcasting logic.
 */
public class NodeBroadcaster {

	private final PeerConnector connector;

	/**
	 * Creates a new broadcaster.
	 *
	 * @param connector The peer connector.
	 */
	public NodeBroadcaster(final PeerConnector connector) {
		this.connector = connector;
	}

	/**
	 * Broadcasts the entity to the target nodes.
	 *
	 * @param partnerNodes The nodes with which to directly communicate.
	 * @param broadcastId The type of entity to broadcast.
	 * @param entity The serializable entity to broadcast.
	 * @return The future.
	 */
	public CompletableFuture<Void> broadcast(final Collection<Node> partnerNodes, final NisPeerId broadcastId,
			final SerializableEntity entity) {

		final List<CompletableFuture<?>> futures = partnerNodes.stream().map(node -> this.connector.announce(node, broadcastId, entity))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
	}
}
