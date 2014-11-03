package org.nem.peer;

import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.time.TimeProvider;
import org.nem.nis.controller.viewmodels.TimeSynchronizationResult;
import org.nem.nis.service.ChainServices;
import org.nem.peer.services.PeerNetworkServicesFactory;
import org.nem.peer.trust.NodeSelector;
import org.nem.peer.trust.score.NodeExperiencesPair;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the NEM network (basically a facade on top of the trust and services packages).
 */
public class PeerNetwork {
	private final PeerNetworkState state;
	private final PeerNetworkServicesFactory servicesFactory;
	private final NodeSelectorFactory selectorFactory;
	private final NodeSelectorFactory importanceAwareSelectorFactory;
	private NodeSelector selector;

	/**
	 * Creates a new network.
	 *
	 * @param state The network state.
	 * @param servicesFactory The network services factory.
	 * @param selectorFactory The node selector factory.
	 */
	public PeerNetwork(
			final PeerNetworkState state,
			final PeerNetworkServicesFactory servicesFactory,
			final NodeSelectorFactory selectorFactory,
			final NodeSelectorFactory importanceAwareSelectorFactory) {
		this.state = state;
		this.servicesFactory = servicesFactory;
		this.selectorFactory = selectorFactory;
		this.importanceAwareSelectorFactory = importanceAwareSelectorFactory;
		this.selector = this.selectorFactory.createNodeSelector();
	}

	//region PeerNetworkState delegation

	/**
	 * Gets a value indication whether or not the local chain is synchronized with the rest of the network.
	 *
	 * @return true if synchronized, false otherwise.
	 */
	public boolean isChainSynchronized() {
		return this.state.isChainSynchronized();
	}

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
	 * Gets a dynamic list of the active nodes to which the running node broadcasts information.
	 *
	 * @return A list of broadcast partners
	 */
	public Collection<Node> getPartnerNodes() {
		return this.selector.selectNodes();
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

	public Collection<TimeSynchronizationResult> getTimeSynchronizationResults() {
		return this.state.getTimeSynchronizationResults();
	}

	//endregion

	//region PeerNetworkServicesFactory delegation

	/**
	 * Refreshes the network.
	 *
	 * @return The future.
	 */
	public CompletableFuture<Void> refresh() {
		return this.servicesFactory.createNodeRefresher().refresh(this.getPartnerNodes())
				.whenComplete((v, e) -> this.selector = this.selectorFactory.createNodeSelector());
	}

	/**
	 * Does one round of network time synchronization.
	 *
	 * @param timeProvider The time provider.
	 * @return The future.
	 */
	public CompletableFuture<Void> synchronizeTime(final TimeProvider timeProvider) {
		return this.servicesFactory.createTimeSynchronizer(this.importanceAwareSelectorFactory.createNodeSelector(), timeProvider).synchronizeTime();
	}

	/**
	 * Broadcasts an entity to all active nodes.
	 *
	 * @param broadcastId The type of entity.
	 * @param entity The entity.
	 * @return The future.
	 */
	public CompletableFuture<Void> broadcast(final NodeApiId broadcastId, final SerializableEntity entity) {
		return this.servicesFactory.createNodeBroadcaster().broadcast(this.getPartnerNodes(), broadcastId, entity);
	}

	/**
	 * Synchronizes this node with another node in the network.
	 */
	public void synchronize() {
		this.servicesFactory.createNodeSynchronizer().synchronize(this.selector);
	}

	/**
	 * Checks if the local chain is synchronized with the rest of the network and updates the network's state.
	 */
	public CompletableFuture<Void> checkChainSynchronization() {
		final ChainServices chainServices = this.servicesFactory.getChainServices();
		return chainServices.isChainSynchronized(this.getNodes().getActiveNodes())
				.thenAccept(this.state::setChainSynchronized);
	}

	/**
	 * Prunes all nodes that have stayed inactive since the last time this function was called.
	 */
	public void pruneInactiveNodes() {
		this.servicesFactory.createInactiveNodePruner().prune(this.state.getNodes());
	}

	/**
	 * Updates the endpoint of the local node as seen by other nodes.
	 */
	public CompletableFuture<Boolean> updateLocalNodeEndpoint() {
		return this.servicesFactory.createLocalNodeEndpointUpdater().update(this.selector);
	}

	//endregion
}
