package org.nem.peer.services;

import org.nem.core.time.TimeProvider;
import org.nem.nis.time.synchronization.*;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.DefaultNodeVersionCheck;
import org.nem.peer.trust.NodeSelector;

/**
 * Factory class for creating peer network services.
 */
public class PeerNetworkServicesFactory {

	private final PeerNetworkState state;
	private final PeerConnector peerConnector;
	private final TimeSynchronizationConnector timeSynchronizationConnector;
	private final SyncConnectorPool syncConnectorPool;
	private final BlockSynchronizer blockSynchronizer;
	private final TimeSynchronizationStrategy timeSyncStrategy;

	/**
	 * Creates a new factory.
	 *
	 * @param state The peer network state.
	 * @param peerConnector The peer connector to use.
	 * @param syncConnectorPool The sync connector pool to use.
	 * @param blockSynchronizer The block synchronizer to use.
	 */
	public PeerNetworkServicesFactory(
			final PeerNetworkState state,
			final PeerConnector peerConnector,
			final TimeSynchronizationConnector timeSynchronizationConnector,
			final SyncConnectorPool syncConnectorPool,
			final BlockSynchronizer blockSynchronizer,
			final TimeSynchronizationStrategy timeSyncStrategy) {
		this.state = state;
		this.peerConnector = peerConnector;
		this.timeSynchronizationConnector = timeSynchronizationConnector;
		this.syncConnectorPool = syncConnectorPool;
		this.blockSynchronizer = blockSynchronizer;
		this.timeSyncStrategy = timeSyncStrategy;
	}

	/**
	 * Creates an inactive node pruner.
	 *
	 * @return An inactive node pruner.
	 */
	public InactiveNodePruner createInactiveNodePruner() {
		return new InactiveNodePruner();
	}

	/**
	 * Creates a local node endpoint updater.
	 *
	 * @return A local node endpoint updater.
	 */
	public LocalNodeEndpointUpdater createLocalNodeEndpointUpdater() {
		return new LocalNodeEndpointUpdater(this.state.getLocalNode(), this.peerConnector);
	}

	/**
	 * Creates a node broadcaster.
	 *
	 * @return A node broadcaster.
	 */
	public NodeBroadcaster createNodeBroadcaster() {
		return new NodeBroadcaster(this.peerConnector);
	}

	/**
	 * Creates a node refresher.
	 *
	 * @return A node refresher.
	 */
	public NodeRefresher createNodeRefresher() {
		return new NodeRefresher(this.state.getLocalNode(), this.state.getNodes(), this.peerConnector, new DefaultNodeVersionCheck());
	}

	/**
	 * Creates a node synchronizer.
	 *
	 * @return A node synchronizer.
	 */
	public NodeSynchronizer createNodeSynchronizer() {
		return new NodeSynchronizer(this.syncConnectorPool, this.blockSynchronizer, this.state);
	}

	/**
	 * Creates a time synchronizer.
	 *
	 * @return A time synchronizer.
	 */
	public TimeSynchronizer createTimeSynchronizer(final NodeSelector selector, final TimeProvider timeProvider) {
		return new NisTimeSynchronizer(selector, this.timeSyncStrategy, this.timeSynchronizationConnector, timeProvider, this.state);
	}
}
