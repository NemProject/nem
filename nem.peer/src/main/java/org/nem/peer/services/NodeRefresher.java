package org.nem.peer.services;

import org.nem.core.connect.*;
import org.nem.core.node.*;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.node.NodeCompatibilityChecker;

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
	private final Set<Node> connectedNodes;
	private final NodeCompatibilityChecker compatibilityChecker;

	/**
	 * Creates a new refresher.
	 *
	 * @param localNode The local node.
	 * @param nodes The network nodes.
	 * @param connector The peer connector.
	 * @param compatibilityChecker The node compatibility checker.
	 */
	public NodeRefresher(final Node localNode, final NodeCollection nodes, final PeerConnector connector,
			final NodeCompatibilityChecker compatibilityChecker) {
		this.localNode = localNode;
		this.nodes = nodes;
		this.connector = connector;
		this.compatibilityChecker = compatibilityChecker;
		this.nodesToUpdate = new ConcurrentHashMap<>();
		this.connectedNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	/**
	 * Refreshes this node's list of active nodes.
	 *
	 * @param refreshNodes The nodes with which to directly communicate.
	 * @return The future.
	 */
	public CompletableFuture<Void> refresh(final Collection<Node> refreshNodes) {
		// all refresh nodes are directly communicated with;
		// ensure that only direct communication is trusted for these nodes
		this.connectedNodes.addAll(refreshNodes);

		final List<CompletableFuture<?>> futures = refreshNodes.stream().map(n -> this.getNodeInfo(n, true)).collect(Collectors.toList());

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).whenComplete((o, e) -> {
			for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet()) {
				this.nodes.update(entry.getKey(), entry.getValue());
			}
		});
	}

	private CompletableFuture<Void> getNodeInfo(final Node node, final boolean isDirectContact) {
		// never sync with the local node or an indirect node that has already been communicated with
		if (this.localNode.equals(node) || (!isDirectContact && !this.connectedNodes.add(node))) {
			return CompletableFuture.completedFuture(null);
		}

		if (this.nodes.isNodeBlacklisted(node)) {
			LOGGER.info(String.format("skipping refresh of blacklisted node: %s", node));
			return CompletableFuture.completedFuture(null);
		}

		CompletableFuture<NodeStatus> future = this.connector.getInfo(node).thenApply(n -> {
			// if the node returned inconsistent information, drop it for this round
			if (!node.equals(n)) {
				throw new FatalPeerException("node response is not compatible with node identity");
			}

			if (!this.compatibilityChecker.check(this.localNode.getMetaData(), n.getMetaData())) {
				throw new FatalPeerException("the local and remote nodes are not compatible");
			}

			node.getIdentity().setName(n.getIdentity().getName());
			node.setEndpoint(n.getEndpoint());
			node.setMetaData(n.getMetaData());
			return NodeStatus.ACTIVE;
		});

		if (isDirectContact) {
			future = future.thenCompose(v -> this.connector.getKnownPeers(node)).thenCompose(nodes -> {
				final List<CompletableFuture<?>> futures = nodes.asCollection().stream().map(n -> this.getNodeInfo(n, false))
						.collect(Collectors.toList());

				return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
			}).thenApply(v -> NodeStatus.ACTIVE).exceptionally(e -> getAndLogStatus(node, "DIRECT", e));
		} else {
			future = future.exceptionally(e -> {
				getAndLogStatus(node, "INDIRECT", e);
				return NodeStatus.UNKNOWN;
			});
		}

		return future.thenAccept(ns -> {
			if (NodeStatus.UNKNOWN != ns) {
				this.update(node, ns);
			}
		});
	}

	private static NodeStatus getAndLogStatus(final Node node, final String qualifier, final Throwable e) {
		final NodeStatus status = getNodeStatusFromException(e);
		logException(node, status, qualifier, e);
		return status;
	}

	private static NodeStatus getNodeStatusFromException(Throwable ex) {
		ex = CompletionException.class == ex.getClass() ? ex.getCause() : ex;
		if (InactivePeerException.class == ex.getClass()) {
			return NodeStatus.INACTIVE;
		} else if (BusyPeerException.class == ex.getClass()) {
			return NodeStatus.BUSY;
		} else {
			return NodeStatus.FAILURE;
		}
	}

	private static void logException(final Node node, final NodeStatus status, final String qualifier, final Throwable e) {
		if (NodeStatus.FAILURE == status) {
			LOGGER.severe(String.format("%s fatal error encountered while communicating with <%s>: %s", qualifier, node, e));
		} else if (NodeStatus.ACTIVE != status) {
			LOGGER.info(String.format("%s warning (%s) encountered while communicating with <%s>: %s", qualifier, status, node, e));
		}
	}

	private void update(final Node node, final NodeStatus status) {
		if (status == this.nodes.getNodeStatus(node) || this.localNode.equals(node)) {
			return;
		}

		LOGGER.info(String.format("Updating \"%s\" -> %s", node, status));
		this.nodesToUpdate.put(node, status);
	}
}
