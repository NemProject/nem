package org.nem.peer;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nem.core.connect.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.connect.*;
import org.nem.peer.node.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.*;

/**
 * Represents a collection of all known NEM nodes.
 */
public class PeerNetwork {

	private static final Logger LOGGER = Logger.getLogger(PeerNetwork.class.getName());

	private final Config config;
	private Node localNode;
	private NodeCollection nodes;
	private final PeerConnector peerConnector;
	private final SyncConnectorPool syncConnectorPool;
	private final BlockSynchronizer blockSynchronizer;

	private final NodeExperiences nodeExperiences;
	private NodeSelector selector;

	/**
	 * Creates a new network with the specified configuration.
	 *
	 * @param config          The network configuration.
	 * @param services        The services to use.
	 * @param nodeExperiences The node experiences to use.
	 * @param nodeCollection  The node collection to use.
	 */
	public PeerNetwork(
			final Config config,
			final PeerNetworkServices services,
			final NodeExperiences nodeExperiences,
			final NodeCollection nodeCollection) {
		this(config, config.getLocalNode(), services, nodeExperiences, nodeCollection);
	}

	private PeerNetwork(
			final Config config,
			final Node localNode,
			final PeerNetworkServices services,
			final NodeExperiences nodeExperiences,
			final NodeCollection nodeCollection) {
		this.config = config;
		this.localNode = localNode;
		this.peerConnector = services.getPeerConnector();
		this.syncConnectorPool = services.getSyncConnectorPool();
		this.blockSynchronizer = services.getBlockSynchronizer();
		this.nodeExperiences = nodeExperiences;
		this.nodes = nodeCollection;

		for (final Node node : config.getPreTrustedNodes().getNodes())
			nodes.update(node, NodeStatus.INACTIVE);
	}

	/**
	 * Creates a new network with the specified configuration.
	 *
	 * @param config   The network configuration.
	 * @param services The services to use.
	 */
	public PeerNetwork(final Config config, final PeerNetworkServices services) {
		this(config, services, new NodeExperiences(), new NodeCollection());
	}

	/**
	 * Creates a new network with the specified configuration and uses a independent trusted node
	 * to verify this node's identity.
	 *
	 * @param config          The network configuration.
	 * @param services        The services to use.
	 */
	public static CompletableFuture<PeerNetwork> createWithVerificationOfLocalNode(
			final Config config,
			final PeerNetworkServices services) {

		LOGGER.log(Level.INFO, "creating a new network with verification of the local node");

		final Node configLocalNode = config.getLocalNode();
		final CompletableFuture<PeerNetwork> networkFuture = new CompletableFuture<>();
		final AtomicInteger numOutstandingRequests = new AtomicInteger(config.getPreTrustedNodes().getSize());
		config.getPreTrustedNodes().getNodes().stream()
				.map(node ->
						services.getPeerConnector().getLocalNodeInfo(node.getEndpoint(), configLocalNode.getEndpoint())
								.exceptionally(e -> null)
								.thenAccept(endpoint -> {
									if (null == endpoint && 0 != numOutstandingRequests.decrementAndGet())
										return;

									networkFuture.complete(new PeerNetwork(
											config,
											getLocalNode(configLocalNode, endpoint),
											services,
											new NodeExperiences(),
											new NodeCollection()));
								}))
				.collect(Collectors.toList());

		return networkFuture;
	}

	private static Node getLocalNode(final Node configLocalNode, final NodeEndpoint reportedEndpoint) {
		LOGGER.info(String.format(
				"local node configured as <%s> seen as <%s>",
				configLocalNode.getEndpoint(),
				reportedEndpoint));

		if (null == reportedEndpoint)
			return configLocalNode;

		return new Node(
				reportedEndpoint,
				configLocalNode.getPlatform(),
				configLocalNode.getApplication(),
				configLocalNode.getVersion());
	}

	/**
	 * Gets the local node.
	 *
	 * @return The local node.
	 */
	public Node getLocalNode() {
		return this.localNode;
	}

	/**
	 * Gets all nodes known to the network.
	 *
	 * @return All nodes known to the network.
	 */
	public NodeCollection getNodes() {
		return this.nodes;
	}

	/**
	 * Gets the local node and information about its current experiences.
	 *
	 * @return The local node and information about its current experiences.
	 */
	public NodeExperiencesPair getLocalNodeAndExperiences() {
		final Node localNode = this.getLocalNode();
		return new NodeExperiencesPair(
				localNode,
				this.nodeExperiences.getNodeExperiences(localNode));
	}

	/**
	 * Sets the experiences for the specified remote node.
	 *
	 * @param pair A node and experiences pair for a remote node.
	 */
	public void setRemoteNodeExperiences(final NodeExperiencesPair pair) {
		if (this.getLocalNode().equals(pair.getNode()))
			throw new IllegalArgumentException("cannot set local node experiences");

		this.nodeExperiences.setNodeExperiences(pair.getNode(), pair.getExperiences());
	}

	private Node[] getNodeArray() {
		return TrustUtils.toNodeArray(this.nodes, this.getLocalNode());
	}

	private void updateTrust() {
		// create a new trust context each iteration in order to allow
		// nodes to change in-between iterations.
		final TrustContext context = new TrustContext(
				this.getNodeArray(),
				this.getLocalNode(),
				this.nodeExperiences,
				this.config.getPreTrustedNodes(),
				this.config.getTrustParameters());

		this.selector = new BasicNodeSelector(
				new ActiveNodeTrustProvider(this.config.getTrustProvider(), this.nodes),
				context);
	}

	/**
	 * Refreshes the network.
	 */
	public CompletableFuture<Void> refresh() {
		final NodeRefresher refresher = new NodeRefresher(
				this.getLocalNode(),
				this.config.getPreTrustedNodes(),
				this.getNodes(),
				this.peerConnector);

		return refresher.refresh().whenComplete((v, e) -> this.updateTrust());
	}

	/**
	 * Broadcasts an entity to all active nodes.
	 *
	 * @param broadcastId The type of entity.
	 * @param entity      The entity.
	 */
	public CompletableFuture<Void> broadcast(final NodeApiId broadcastId, final SerializableEntity entity) {
		final List<CompletableFuture> futures = this.nodes.getActiveNodes().stream()
				.map(node -> this.peerConnector.announce(node.getEndpoint(), broadcastId, entity))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
	}

	/**
	 * Synchronizes this node with another node in the network.
	 */
	public void synchronize() {
		final NodeExperiencePair partnerNodePair = this.selector.selectNode();
		if (null == partnerNodePair) {
			LOGGER.warning("no suitable peers found to sync with");
			return;
		}

		final Node partnerNode = partnerNodePair.getNode();
		LOGGER.info("synchronizing with: " + partnerNode);
		final NodeInteractionResult result = this.blockSynchronizer.synchronizeNode(this.syncConnectorPool, partnerNode);
		LOGGER.info("synchronize with    " + partnerNode + " finished.");
		this.updateExperience(partnerNodePair.getNode(), result);
	}

	/**
	 * Updates the local node's experience with the specified node.
	 *
	 * @param node The remote node that was interacted with.
	 * @param result The interaction result.
	 */
	public void updateExperience(final Node node, final NodeInteractionResult result) {
		if (NodeInteractionResult.NEUTRAL == result || node.equals(this.localNode))
			return;

		final NodeExperience experience = this.nodeExperiences.getNodeExperience(this.localNode, node);
		(NodeInteractionResult.SUCCESS == result ? experience.successfulCalls() : experience.failedCalls()).increment();
		LOGGER.info(String.format("Updating experience with %s: %s", node, result));
	}

	/**
	 * Prunes all nodes that have stayed inactive since the last time this function was called.
	 */
	public void pruneInactiveNodes() {
		LOGGER.fine("pruning inactive nodes");
		final int numInitialInactiveNodes = this.nodes.getInactiveNodes().size();
		this.nodes.pruneInactiveNodes();
		final int numNodesPruned = numInitialInactiveNodes - this.nodes.getInactiveNodes().size();
		LOGGER.info(String.format("%d inactive node(s) were pruned", numNodesPruned));
	}

	/**
	 * Update the endpoint of the local node as seen by other nodes.
	 */
	public void updateLocalNodeEndpoint() {
		LOGGER.info("updating local node.");
		final Node configLocalNode = config.getLocalNode();
		final NodeExperiencePair partnerNodePair = this.selector.selectNode();
		if (partnerNodePair == null) {
			LOGGER.warning("no suitable peers found to update local node.");
		}
		this.peerConnector.getLocalNodeInfo(partnerNodePair.getNode().getEndpoint(), configLocalNode.getEndpoint())
						  .exceptionally(e -> null)
						  .thenAccept(endpoint -> { LOGGER.info(String.format(
																"local node configured as <%s> seen as <%s>",
																configLocalNode.getEndpoint(),
																endpoint));
						  							this.localNode.setEndpoint(endpoint); 
						  							config.updateLocalNodeEndpoint(endpoint); });
	}

	private static class NodeRefresher {
		final Node localNode;
		final PreTrustedNodes preTrustedNodes;
		final NodeCollection nodes;
		final PeerConnector connector;
		final Map<Node, NodeStatus> nodesToUpdate;
		final ConcurrentHashSet<Node> connectedNodes;

		public NodeRefresher(
				final Node localNode,
				final PreTrustedNodes preTrustedNodes,
				final NodeCollection nodes,
				final PeerConnector connector) {
			this.localNode = localNode;
			this.preTrustedNodes = preTrustedNodes;
			this.nodes = nodes;
			this.connector = connector;
			this.nodesToUpdate = new ConcurrentHashMap<>();
			this.connectedNodes = new ConcurrentHashSet<>();
		}

		public CompletableFuture<Void> refresh() {
			final List<CompletableFuture> futures = this.getRefreshNodes()
					.map(this::refreshNodeAsync)
					.collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                    .whenComplete((o, e) -> {
						for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet())
							this.nodes.update(entry.getKey(), entry.getValue());
					});
		}

		private Stream<Node> getRefreshNodes() {
			// always include pre-trusted nodes even if they previously resulted in a failure
			final Set<Node> refreshNodes = new HashSet<>(this.nodes.getAllNodes());
			refreshNodes.addAll(this.preTrustedNodes.getNodes().stream().collect(Collectors.toList()));
			return refreshNodes.stream();
		}

		private CompletableFuture<Void> refreshNodeAsync(final Node node) {
			return this.getNodeInfo(node, true);
		}

		private CompletableFuture<Void> getNodeInfo(final Node node, boolean isDirectContact) {
			if (!this.connectedNodes.add(node)) {
				return CompletableFuture.completedFuture(null);
			}

			CompletableFuture<NodeStatus> future = this.connector.getInfo(node.getEndpoint())
					.thenApply(n -> {
						// if the node returned inconsistent information, drop it for this round
						if (!areCompatible(node, n))
							throw new FatalPeerException("node response is not compatible with node identity");

						return NodeStatus.ACTIVE;
					});

			if (isDirectContact) {
				future = future
						.thenCompose(v -> this.connector.getKnownPeers(node.getEndpoint()))
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

		private static boolean areCompatible(final Node lhs, final Node rhs) {
			return lhs.equals(rhs);
		}

		private void update(final Node node, final NodeStatus status) {
			if (status == this.nodes.getNodeStatus(node) || this.localNode.equals(node))
				return;

			LOGGER.info(String.format("Updating \"%s\" -> %s", node, status));
			this.nodesToUpdate.put(node, status);
		}
	}
}
