package org.nem.peer;

import org.nem.core.connect.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.node.*;
import org.nem.peer.scheduling.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;

/**
 * Represents a collection of all known NEM nodes.
 */
public class PeerNetwork {

	private final Config config;
	private NodeCollection nodes;
	private final PeerConnector peerConnector;
	private final SyncConnectorPool syncConnectorPool;
	private final SchedulerFactory<Node> schedulerFactory;
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
		this.schedulerFactory = services.getSchedulerFactory();
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
	public void refresh() {
		final NodeRefresher refresher = new NodeRefresher(
				this.getLocalNode(),
				this.getNodes(),
				this.peerConnector,
				this.schedulerFactory);
		refresher.refresh();
	}

	/**
	 * Broadcasts an entity to all active nodes.
	 *
	 * @param broadcastId The type of entity.
	 * @param entity      The entity.
	 */
	public void broadcast(final NodeApiId broadcastId, final SerializableEntity entity) {
		this.forAllActiveNodes(element -> peerConnector.announce(element.getEndpoint(), broadcastId, entity));
	}

	public void synchronize() {
		this.forAllActiveNodes(element -> blockSynchronizer.synchronizeNode(syncConnectorPool, element));
	}

	private void forAllActiveNodes(final Action<Node> action) {
		final Scheduler<Node> scheduler = this.schedulerFactory.createScheduler(action);
		scheduler.push(this.nodes.getActiveNodes());
		scheduler.block();
	}

	private static class NodeRefresher {
		final Node localNode;
		final NodeCollection nodes;
		final PeerConnector connector;
		final SchedulerFactory<Node> schedulerFactory;
		final Map<Node, NodeStatus> nodesToUpdate;

		public NodeRefresher(
				final Node localNode,
				final NodeCollection nodes,
				final PeerConnector connector,
				final SchedulerFactory<Node> schedulerFactory) {
			this.localNode = localNode;
			this.nodes = nodes;
			this.connector = connector;
			this.schedulerFactory = schedulerFactory;
			this.nodesToUpdate = new HashMap<>();
		}

		public void refresh() {
			Scheduler<Node> scheduler = this.schedulerFactory.createScheduler(this::refreshNode);

			scheduler.push(this.nodes.getActiveNodes());
			scheduler.push(this.nodes.getInactiveNodes());
			scheduler.block();

			for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet())
				this.nodes.update(entry.getKey(), entry.getValue());
		}

		private void refreshNode(final Node node) {
			Node refreshedNode = node;
			NodeStatus updatedStatus = NodeStatus.ACTIVE;
			try {
				refreshedNode = this.connector.getInfo(node.getEndpoint());

				// if the node returned inconsistent information, drop it for this round
				if (!areCompatible(node, refreshedNode)) {
					updatedStatus = NodeStatus.FAILURE;
					refreshedNode = node;
				} else {
					this.mergePeers(this.connector.getKnownPeers(node.getEndpoint()));
				}
			} catch (InactivePeerException e) {
				updatedStatus = NodeStatus.INACTIVE;
			} catch (FatalPeerException e) {
				updatedStatus = NodeStatus.FAILURE;
			}

			this.update(refreshedNode, updatedStatus);
		}

		private static boolean areCompatible(final Node lhs, final Node rhs) {
			return lhs.equals(rhs);
		}

		private void update(final Node node, final NodeStatus status) {
			if (status == this.nodes.getNodeStatus(node) || this.localNode.equals(node))
				return;

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
				if (NodeStatus.FAILURE != this.nodes.getNodeStatus(node))
					continue;

				this.update(node, status);
			}
		}
	}
}
