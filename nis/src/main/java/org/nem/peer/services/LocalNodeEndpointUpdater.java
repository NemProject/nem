package org.nem.peer.services;

import org.nem.core.node.Node;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.trust.NodeSelector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Helper class used to implement local node endpoint updating logic.
 */
public class LocalNodeEndpointUpdater {
	private static final Logger LOGGER = Logger.getLogger(LocalNodeEndpointUpdater.class.getName());

	private final Node localNode;
	private final PeerConnector connector;

	/**
	 * Creates a local node endpoint updater.
	 *
	 * @param localNode The local node.
	 * @param connector The peer connector.
	 */
	public LocalNodeEndpointUpdater(final Node localNode, final PeerConnector connector) {
		this.localNode = localNode;
		this.connector = connector;
	}

	/**
	 * Updates the local node endpoint.
	 *
	 * @param selector The node selector.
	 * @return The future (true if the node was updated; false otherwise).
	 */
	public CompletableFuture<Boolean> update(final NodeSelector selector) {
		LOGGER.info("updating local node endpoint");
		final Node partnerNode = selector.selectNode();
		if (null == partnerNode) {
			LOGGER.warning("no suitable peers found to update local node");
			return CompletableFuture.completedFuture(false);
		}

		return this.update(partnerNode);
	}

	private CompletableFuture<Boolean> update(final Node node) {
		return this.connector.getLocalNodeInfo(node, this.localNode.getEndpoint())
				.handle((endpoint, e) -> {
					if (null == endpoint) {
						return false;
					}

					if (this.localNode.getEndpoint().equals(endpoint)) {
						return true;
					}

					LOGGER.info(String.format("updating local node endpoint from <%s> to <%s>",
							this.localNode.getEndpoint(),
							endpoint));
					this.localNode.setEndpoint(endpoint);
					return true;
				});
	}

	/**
	 * Updates the local node endpoint by using any of the specified nodes.
	 *
	 * @param nodes The candidate nodes.
	 * @return The future (true if the node was updated by at least one node; false otherwise).
	 */
	public CompletableFuture<Boolean> updateAny(final Collection<Node> nodes) {
		final CompletableFuture<Boolean> aggregateFuture = new CompletableFuture<>();
		final List<CompletableFuture<?>> futures = nodes.stream()
				.map(node -> this.update(node).thenAccept(b -> { if (b) { aggregateFuture.complete(true); } }))
				.collect(Collectors.toList());

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
				.thenAccept(b -> aggregateFuture.complete(false));

		return aggregateFuture;
	}
}
