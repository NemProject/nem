package org.nem.peer.services;

import org.nem.core.node.*;
import org.nem.peer.connect.PeerConnector;

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
	 * @param node The remote node.
	 * @return The future (true if the node was updated; false otherwise).
	 */
	public CompletableFuture<Boolean> update(final Node node) {
		return this.getLocalEndpoint(node).thenApply(this::setLocalEndpoint);
	}

	/**
	 * Updates the local node endpoint by using the endpoint returned by the plurality the specified nodes.
	 *
	 * @param nodes The candidate nodes.
	 * @return The future (true if the node was updated; false otherwise).
	 */
	public CompletableFuture<Boolean> updatePlurality(final Collection<Node> nodes) {
		final NodeEndpoint[] endpoints = new NodeEndpoint[nodes.size()];
		final List<CompletableFuture<?>> futures = new ArrayList<>(nodes.size());
		final Iterator<Node> iterator = nodes.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			final int j = i++;
			final Node node = iterator.next();
			final CompletableFuture<?> future = this.getLocalEndpoint(node).thenAccept(endpoint -> endpoints[j] = endpoint);

			futures.add(future);
		}

		final CompletableFuture<Boolean> aggregateFuture = new CompletableFuture<>();
		CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]))
				.thenAccept(v -> aggregateFuture.complete(this.setLocalEndpoint(findBestEndpoint(endpoints))));
		return aggregateFuture;
	}

	private static NodeEndpoint findBestEndpoint(final NodeEndpoint[] endpoints) {
		final Map<NodeEndpoint, Integer> endpointCounts = new HashMap<>();
		for (final NodeEndpoint endpoint : endpoints) {
			if (null != endpoint) {
				endpointCounts.put(endpoint, endpointCounts.getOrDefault(endpoint, 0) + 1);
			}
		}

		NodeEndpoint bestEndpoint = null;
		int maxCount = 0;
		for (final Map.Entry<NodeEndpoint, Integer> entry : endpointCounts.entrySet()) {
			if (entry.getValue() > maxCount) {
				maxCount = entry.getValue();
				bestEndpoint = entry.getKey();
			}
		}

		return bestEndpoint;
	}

	/**
	 * Updates the local node endpoint by using any of the specified nodes.
	 *
	 * @param nodes The candidate nodes.
	 * @return The future (true if the node was updated by at least one node; false otherwise).
	 */
	public CompletableFuture<Boolean> updateAny(final Collection<Node> nodes) {
		final CompletableFuture<Boolean> aggregateFuture = new CompletableFuture<>();
		final List<CompletableFuture<?>> futures = nodes.stream().map(node -> this.getLocalEndpoint(node).thenAccept(endpoint -> {
			if (!aggregateFuture.isDone() && this.setLocalEndpoint(endpoint)) {
				aggregateFuture.complete(true);
			}
		})).collect(Collectors.toList());

		CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).thenAccept(b -> aggregateFuture.complete(false));

		return aggregateFuture;
	}

	private CompletableFuture<NodeEndpoint> getLocalEndpoint(final Node node) {
		return this.connector.getLocalNodeInfo(node, this.localNode.getEndpoint()).handle((endpoint, e) -> endpoint);
	}

	private Boolean setLocalEndpoint(final NodeEndpoint endpoint) {
		if (null == endpoint) {
			return false;
		}

		if (this.localNode.getEndpoint().equals(endpoint)) {
			return true;
		}

		LOGGER.info(String.format("updating local node endpoint from <%s> to <%s>", this.localNode.getEndpoint(), endpoint));
		this.localNode.setEndpoint(endpoint);
		return true;
	}
}
