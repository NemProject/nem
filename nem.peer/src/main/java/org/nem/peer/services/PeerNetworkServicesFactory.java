package org.nem.peer.services;

import org.nem.core.time.TimeProvider;
import org.nem.core.time.synchronization.TimeSynchronizer;
import org.nem.peer.trust.NodeSelector;

/**
 * Interface for providing peer network services.
 */
public interface PeerNetworkServicesFactory {

	/**
	 * Creates an inactive node pruner.
	 *
	 * @return An inactive node pruner.
	 */
	InactiveNodePruner createInactiveNodePruner();

	/**
	 * Creates a local node endpoint updater.
	 *
	 * @return A local node endpoint updater.
	 */
	LocalNodeEndpointUpdater createLocalNodeEndpointUpdater();

	/**
	 * Creates a node broadcaster.
	 *
	 * @return A node broadcaster.
	 */
	NodeBroadcaster createNodeBroadcaster();

	/**
	 * Creates a node refresher.
	 *
	 * @return A node refresher.
	 */
	NodeRefresher createNodeRefresher();

	/**
	 * Creates a node synchronizer.
	 *
	 * @return A node synchronizer.
	 */
	NodeSynchronizer createNodeSynchronizer();

	/**
	 * Gets the chain services.
	 *
	 * @return The chain services.
	 */
	ChainServices getChainServices();

	/**
	 * Creates a time synchronizer.
	 *
	 * @param selector The node selector.
	 * @param timeProvider The time provider.
	 * @return A time synchronizer.
	 */
	TimeSynchronizer createTimeSynchronizer(final NodeSelector selector, final TimeProvider timeProvider);

	/**
	 * Creates a node experience updater.
	 *
	 * @param timeProvider The time provider.
	 * @return A node experience updater.
	 */
	NodeExperiencesUpdater createNodeExperiencesUpdater(final TimeProvider timeProvider);
}
