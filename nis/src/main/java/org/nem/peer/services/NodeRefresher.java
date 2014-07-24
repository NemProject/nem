package org.nem.peer.services;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.connect.*;
import org.nem.core.node.Node;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.node.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Helper class used to implement network refreshing logic.
 */
public class NodeRefresher {
	private static final Logger LOGGER = Logger.getLogger(NodeRefresher.class.getName());

	private final Node localNode;
	private final NodeCollection nodes;
	private final PeerConnector connector;
	private final Map<Node, NodeStatus> nodesToUpdate;
	private final ConcurrentHashSet<Node> connectedNodes;
	private final NodeVersionCheck versionCheck;

	/**
	 * Creates a new refresher.
	 *
	 * @param localNode The local node.
	 * @param nodes The network nodes.
	 * @param connector The peer connector.
	 */
	public NodeRefresher(
			final Node localNode,
			final NodeCollection nodes,
			final PeerConnector connector,
			final NodeVersionCheck versionCheck) {
		this.localNode = localNode;
		this.nodes = nodes;
		this.connector = connector;
		this.versionCheck = versionCheck;
		this.nodesToUpdate = new ConcurrentHashMap<>();
		this.connectedNodes = new ConcurrentHashSet<>();
	}

	/**
	 * Refreshes this node's list of active nodes.
	 *
	 * @param refreshNodes The nodes with which to directly communicate.
	 * @return The future.
	 */
	public CompletableFuture<Void> refresh(final List<Node> refreshNodes) {
		// all refresh nodes are directly communicated with;
		// ensure that only direct communication is trusted for these nodes
		this.connectedNodes.addAll(refreshNodes);

		final List<CompletableFuture> futures = refreshNodes.stream()
				.map(n -> this.getNodeInfo(n, true))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
				.whenComplete((o, e) -> {
					for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet())
						this.nodes.update(entry.getKey(), entry.getValue());
				});
	}

	private CompletableFuture<Void> getNodeInfo(final Node node, boolean isDirectContact) {
		// never sync with the local node or an indirect node that has already been communicated with
		if (this.localNode.equals(node) || (!isDirectContact && !this.connectedNodes.add(node))) {
			return CompletableFuture.completedFuture(null);
		}

		CompletableFuture<NodeStatus> future = this.connector.getInfo(node)
				.thenApply(n -> {
					// if the node returned inconsistent information, drop it for this round
					if (!node.equals(n))
						throw new FatalPeerException("node response is not compatible with node identity");

					if (!this.versionCheck.check(this.localNode.getMetaData().getVersion(), n.getMetaData().getVersion()))
						throw new FatalPeerException("the local and remote nodes are not compatible");

					node.setEndpoint(n.getEndpoint());
					node.setMetaData(n.getMetaData());
					return NodeStatus.ACTIVE;
				});

		if (isDirectContact) {
			future = future
					.thenCompose(v -> this.connector.getKnownPeers(node))
					.thenCompose(nodes -> {
						final List<CompletableFuture> futures = nodes.asCollection().stream()
								.map(n -> this.getNodeInfo(n, false))
								.collect(Collectors.toList());

						return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
					})
					.thenApply(v -> NodeStatus.ACTIVE);
		}

		return future
				.exceptionally(this::getNodeStatusFromException)
				.thenAccept(ns -> this.update(node, ns));
	}

	private NodeStatus getNodeStatusFromException(Throwable ex) {
		ex = CompletionException.class == ex.getClass() ? ex.getCause() : ex;
		return InactivePeerException.class == ex.getClass() ? NodeStatus.INACTIVE : NodeStatus.FAILURE;
	}

	private void update(final Node node, final NodeStatus status) {
		if (status == this.nodes.getNodeStatus(node) || this.localNode.equals(node))
			return;

		LOGGER.info(String.format("Updating \"%s\" -> %s", node, status));
		this.nodesToUpdate.put(node, status);
	}
}