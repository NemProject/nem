package org.nem.peer.services;

import org.nem.peer.connect.PeerConnector;
import org.nem.peer.node.*;
import org.nem.peer.trust.NodeSelector;

import java.util.concurrent.*;
import java.util.logging.Logger;

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
	 * @return The future.
	 */
	public CompletableFuture<Boolean> update(final NodeSelector selector) {
		LOGGER.info("updating local node endpoint");
		final Node partnerNode = selector.selectNode();
		if (null == partnerNode) {
			LOGGER.warning("no suitable peers found to update local node");
			return CompletableFuture.completedFuture(false);
		}

		return this.connector.getLocalNodeInfo(partnerNode, this.localNode.getEndpoint())
				.handle((endpoint, e) -> {
					if (null == endpoint)
						return false;

					if (this.localNode.getEndpoint().equals(endpoint))
						return true;

					LOGGER.info(String.format("updating local node endpoint from <%s> to <%s>",
							this.localNode.getEndpoint(),
							endpoint));
					this.localNode.setEndpoint(endpoint);
					return true;
				});
	}
}
