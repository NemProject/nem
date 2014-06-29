package org.nem.peer;

import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.services.*;
import org.nem.peer.node.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * Represents the NEM network (basically a facade on top of the trust and services packages).
 */
public class PeerNetwork {
	private final PeerNetworkState state;
	private final PeerNetworkServicesFactory factory;
	private NodeSelector selector;

	/**
	 * Creates a new network.
	 *
	 * @param state The network state.
	 * @param factory The network services factory.
	 */
	public PeerNetwork(
			final PeerNetworkState state,
			final PeerNetworkServicesFactory factory) {
		this.state = state;
		this.factory = factory;
		this.selector = this.factory.createNodeSelector();
	}

	//region PeerNetworkState delegation

	/**
	 * Gets the local node.
	 *
	 * @return The local node.
	 */
	public Node getLocalNode() {
		return this.state.getLocalNode();
	}

	/**
	 * Gets all nodes known to the network.
	 *
	 * @return All nodes known to the network.
	 */
	public NodeCollection getNodes() {
		return this.state.getNodes();
	}

	/**
	 * Gets the local node and information about its current experiences.
	 *
	 * @return The local node and information about its current experiences.
	 */
	public NodeExperiencesPair getLocalNodeAndExperiences() {
		return this.state.getLocalNodeAndExperiences();
	}

	/**
	 * Updates the local node's experience with the specified node.
	 *
	 * @param node The remote node that was interacted with.
	 * @param result The interaction result.
	 */
	public void updateExperience(final Node node, final NodeInteractionResult result) {
		this.state.updateExperience(node, result);
	}

	/**
	 * Sets the experiences for the specified remote node.
	 *
	 * @param pair A node and experiences pair for a remote node.
	 */
	public void setRemoteNodeExperiences(final NodeExperiencesPair pair) {
		this.state.setRemoteNodeExperiences(pair);
	}

	//endregion

	//region PeerNetworkServicesFactory delegation

	/**
	 * Refreshes the network.
	 */
	public CompletableFuture<Void> refresh() {
		return this.factory.createNodeRefresher().refresh(this.getPartnerNodes())
				.whenComplete((v, e) -> this.selector = this.factory.createNodeSelector());
	}

	/**
	 * Broadcasts an entity to all active nodes.
	 *
	 * @param broadcastId The type of entity.
	 * @param entity      The entity.
	 */
	public CompletableFuture<Void> broadcast(final NodeApiId broadcastId, final SerializableEntity entity) {
		return this.factory.createNodeBroadcaster().broadcast(this.getPartnerNodes(), broadcastId, entity);
	}

	/**
	 * Synchronizes this node with another node in the network.
	 */
	public void synchronize() {
		this.factory.createNodeSynchronizer().synchronize(this.selector);
	}

	/**
	 * Prunes all nodes that have stayed inactive since the last time this function was called.
	 */
	public void pruneInactiveNodes() {
		this.factory.createInactiveNodePruner().prune(this.state.getNodes());
	}

	/**
	 * Updates the endpoint of the local node as seen by other nodes.
	 */
	public CompletableFuture<Boolean> updateLocalNodeEndpoint() {
		return this.factory.createLocalNodeEndpointUpdater().update(this.selector);
	}

	private List<Node> getPartnerNodes() {
		return this.selector.selectNodes(10).stream()
				.collect(Collectors.toList());
	}

	//endregion
}
