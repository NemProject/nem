package org.nem.peer;

import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.time.*;
import org.nem.peer.services.*;
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
	private final PeerNetworkNodeSelectorFactory selectorFactory;
	private NodeSelector selector;

	/**
	 * Creates a new network.
	 *
	 * @param state The network state.
	 * @param servicesFactory The network services factory.
	 * @param selectorFactory The node selector factory.
	 */
	public PeerNetwork(final PeerNetworkState state, final PeerNetworkServicesFactory servicesFactory,
			final PeerNetworkNodeSelectorFactory selectorFactory) {
		this.state = state;
		this.servicesFactory = servicesFactory;
		this.selectorFactory = selectorFactory;
		this.selector = this.selectorFactory.createUpdateNodeSelector();
	}

	// region PeerNetworkState delegation

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
	 * @return A list of broadcast partners.
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

	public Collection<TimeSynchronizationResult> getTimeSynchronizationResults() {
		return this.state.getTimeSynchronizationResults();
	}

	// endregion

	// region PeerNetworkServicesFactory delegation

	/**
	 * Refreshes the network.
	 *
	 * @return The future.
	 */
	public CompletableFuture<Void> refresh() {
		final Collection<Node> refreshNodes = this.selectorFactory.createRefreshNodeSelector().selectNodes();
		return this.servicesFactory.createNodeRefresher().refresh(refreshNodes)
				.whenComplete((v, e) -> this.selector = this.selectorFactory.createUpdateNodeSelector());
	}

	/**
	 * Does one round of network time synchronization.
	 *
	 * @param timeProvider The time provider.
	 * @return The future.
	 */
	public CompletableFuture<Void> synchronizeTime(final TimeProvider timeProvider) {
		final NodeSelector selector = this.selectorFactory.createTimeSyncNodeSelector();
		return this.servicesFactory.createTimeSynchronizer(selector, timeProvider).synchronizeTime();
	}

	/**
	 * Broadcasts an entity to all active nodes.
	 *
	 * @param broadcastId The type of entity.
	 * @param entity The entity.
	 * @return The future.
	 */
	public CompletableFuture<Void> broadcast(final NisPeerId broadcastId, final SerializableEntity entity) {
		return this.servicesFactory.createNodeBroadcaster().broadcast(this.getPartnerNodes(), broadcastId, entity);
	}

	/**
	 * Synchronizes this node with another node in the network.
	 */
	public void synchronize() {
		this.servicesFactory.createNodeSynchronizer().synchronize(this.selector);
	}

	/**
	 * Pulls experiences from a peer and updates the node experiences.
	 *
	 * @param timeProvider The time provider.
	 * @return The future.
	 */
	public CompletableFuture<Boolean> updateNodeExperiences(final TimeProvider timeProvider) {
		final NodeSelector selector = this.selectorFactory.createUpdateNodeSelector();
		return this.servicesFactory.createNodeExperiencesUpdater(timeProvider).update(selector);
	}

	/**
	 * Prunes the node experiences according to the given timestamp.
	 *
	 * @param currentTime The current time.
	 */
	public void pruneNodeExperiences(final TimeInstant currentTime) {
		this.state.pruneNodeExperiences(currentTime);
	}

	/**
	 * Checks if the local chain is synchronized with the rest of the network and updates the network's state.
	 *
	 * @return Void future.
	 */
	public CompletableFuture<Void> checkChainSynchronization() {
		final ChainServices chainServices = this.servicesFactory.getChainServices();
		return chainServices.isChainSynchronized(this.getPartnerNodes()).thenAccept(this.state::setChainSynchronized);
	}

	/**
	 * Prunes all nodes that have stayed inactive since the last time this function was called.
	 */
	public void pruneInactiveNodes() {
		this.servicesFactory.createInactiveNodePruner().prune(this.state.getNodes());
	}

	/**
	 * Updates the endpoint of the local node as seen by other nodes.
	 *
	 * @return True if the node was updated; false otherwise.
	 */
	public CompletableFuture<Boolean> updateLocalNodeEndpoint() {
		return this.servicesFactory.createLocalNodeEndpointUpdater().updatePlurality(this.getPartnerNodes());
	}

	/**
	 * Boots the local node.
	 *
	 * @return True if the node was booted; false otherwise.
	 */
	public CompletableFuture<Boolean> boot() {
		// it is safe to use partner nodes before a refresh cycle
		return this.servicesFactory.createLocalNodeEndpointUpdater().updateAny(this.getPartnerNodes());
	}

	// endregion
}
