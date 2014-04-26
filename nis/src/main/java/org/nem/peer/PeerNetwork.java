package org.nem.peer;

import org.nem.core.connect.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.node.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents a collection of all known NEM nodes.
 */
public class PeerNetwork {

	private static final Logger LOGGER = Logger.getLogger(PeerNetwork.class.getName());

	private final Config config;
	private NodeCollection nodes;
	private final PeerConnector peerConnector;
	private final SyncConnectorPool syncConnectorPool;
	private final BlockSynchronizer blockSynchronizer;

	private final NodeExperiences nodeExperiences;

	/**
	 * Creates a new network with the specified configuration.
	 *
	 * @param config          The network configuration.
	 * @param services        The services to use.
	 * @param nodeExperiences The node experiences to use.
	 */
	public PeerNetwork(
			final Config config,
			final PeerNetworkServices services,
			final NodeExperiences nodeExperiences) {

		this.config = config;
		this.nodes = new NodeCollection();
		this.peerConnector = services.getPeerConnector();
		this.syncConnectorPool = services.getSyncConnectorPool();
		this.blockSynchronizer = services.getBlockSynchronizer();
		this.nodeExperiences = nodeExperiences;

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
		this(config, services, new NodeExperiences());
	}

	/**
	 * Gets the local node.
	 *
	 * @return The local node.
	 */
	public Node getLocalNode() {
		return this.config.getLocalNode();
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

	/**
	 * Gets a communication partner node.
	 * TODO: with this model the EigenTrust trust will be calculated each time a partner is requested.
	 *
	 * @return A communication partner node.
	 */
	public NodeExperiencePair getPartnerNode() {
		// create a new trust context each iteration in order to allow
		// nodes to change in-between iterations.
		final TrustContext context = new TrustContext(
				this.getNodeArray(),
				this.getLocalNode(),
				this.nodeExperiences,
				this.config.getPreTrustedNodes(),
				this.config.getTrustParameters());

		final NodeSelector basicNodeSelector = this.getNodeSelector();
		return basicNodeSelector.selectNode(context);
	}

	private Node[] getNodeArray() {
		return TrustUtils.toNodeArray(this.nodes, this.getLocalNode());
	}

	private NodeSelector getNodeSelector() {
		// wrap the configured trust provider in an ActiveNodeTrustProvider to ensure that
		// only active nodes are returned as communication partners
		return new BasicNodeSelector(new ActiveNodeTrustProvider(config.getTrustProvider(), this.nodes));
	}

	/**
	 * Refreshes the network.
	 */
	public CompletableFuture refresh() {
		final NodeRefresher refresher = new NodeRefresher(
				this.getLocalNode(),
				this.getNodes(),
				this.peerConnector);

		return refresher.refresh();
	}

	/**
	 * Broadcasts an entity to all active nodes.
	 *
	 * @param broadcastId The type of entity.
	 * @param entity      The entity.
	 */
	public CompletableFuture broadcast(final NodeApiId broadcastId, final SerializableEntity entity) {
		final List<CompletableFuture> futures = this.nodes.getActiveNodes().stream()
				.map(node -> this.peerConnector.announce(node.getEndpoint(), broadcastId, entity))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
	}

	public void synchronize() {
		this.blockSynchronizer.synchronizeNode(this.syncConnectorPool, this.getPartnerNode().getNode());
	}

	private static class NodeRefresher {
		final Node localNode;
		final NodeCollection nodes;
		final PeerConnector connector;
		final Map<Node, NodeStatus> nodesToUpdate;

		public NodeRefresher(
				final Node localNode,
				final NodeCollection nodes,
				final PeerConnector connector) {
			this.localNode = localNode;
			this.nodes = nodes;
			this.connector = connector;
			this.nodesToUpdate = new ConcurrentHashMap<>();
		}

		public CompletableFuture refresh() {
			final List<CompletableFuture> futures = this.nodes.getAllNodes().stream()
					.map(this::refreshNodeAsync)
					.collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                    .whenComplete((o, e) -> {
						for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet())
							this.nodes.update(entry.getKey(), entry.getValue());
					});
		}

		private CompletableFuture refreshNodeAsync(final Node node) {

			return this.connector.getInfo(node.getEndpoint())
					.thenApply(n -> {
						// if the node returned inconsistent information, drop it for this round
						if (!areCompatible(node, n))
							throw new FatalPeerException("node response is not compatible with node identity");

						return NodeStatus.ACTIVE;
					})
					.thenCompose(ns -> this.connector.getKnownPeers(node.getEndpoint()))
					.thenApply(nodes -> {
						this.mergePeers(nodes);
						return NodeStatus.ACTIVE;
					})
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

			LOGGER.info("Updating \"" + node + "\" -> " + status);
			this.nodesToUpdate.put(node, status);
		}

		private void mergePeers(final NodeCollection nodes) {
			this.mergePeers(nodes.getActiveNodes(), NodeStatus.ACTIVE);
			this.mergePeers(nodes.getInactiveNodes(), NodeStatus.INACTIVE);
		}

		private void mergePeers(final Iterable<Node> iterable, final NodeStatus status) {
			for (final Node node : iterable) {
				// nodes directly communicated with are already in this.nodes
				// give their direct connection precedence over what peers report
				LOGGER.fine("Merging Peer \"" + node + "\" -> " + status);
				if (NodeStatus.FAILURE != this.nodes.getNodeStatus(node))
					continue;

				this.update(node, status);
			}
		}
	}
}
