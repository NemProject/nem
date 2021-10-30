package org.nem.nis.connect;

import org.nem.core.time.TimeProvider;
import org.nem.core.time.synchronization.TimeSynchronizer;
import org.nem.nis.time.synchronization.*;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.NodeCompatibilityChecker;
import org.nem.peer.services.*;
import org.nem.peer.trust.NodeSelector;

/**
 * Factory class for creating peer network services.
 */
public class DefaultPeerNetworkServicesFactory implements PeerNetworkServicesFactory {
	private final PeerNetworkState state;
	private final PeerConnector peerConnector;
	private final TimeSynchronizationConnector timeSynchronizationConnector;
	private final SyncConnectorPool syncConnectorPool;
	private final BlockSynchronizer blockSynchronizer;
	private final ChainServices chainServices;
	private final TimeSynchronizationStrategy timeSyncStrategy;
	private final NodeCompatibilityChecker compatibilityChecker;

	/**
	 * Creates a new factory.
	 *
	 * @param state The peer network state.
	 * @param peerConnector The peer connector to use.
	 * @param timeSynchronizationConnector The time sync connector to use.
	 * @param syncConnectorPool The sync connector pool to use.
	 * @param blockSynchronizer The block synchronizer to use.
	 * @param chainServices The chain services to use.
	 * @param timeSyncStrategy The time sync strategy to use.
	 * @param compatibilityChecker The node compatibility checker.
	 */
	public DefaultPeerNetworkServicesFactory(final PeerNetworkState state, final PeerConnector peerConnector,
			final TimeSynchronizationConnector timeSynchronizationConnector, final SyncConnectorPool syncConnectorPool,
			final BlockSynchronizer blockSynchronizer, final ChainServices chainServices,
			final TimeSynchronizationStrategy timeSyncStrategy, final NodeCompatibilityChecker compatibilityChecker) {
		this.state = state;
		this.peerConnector = peerConnector;
		this.timeSynchronizationConnector = timeSynchronizationConnector;
		this.syncConnectorPool = syncConnectorPool;
		this.blockSynchronizer = blockSynchronizer;
		this.chainServices = chainServices;
		this.timeSyncStrategy = timeSyncStrategy;
		this.compatibilityChecker = compatibilityChecker;
	}

	@Override
	public InactiveNodePruner createInactiveNodePruner() {
		return new InactiveNodePruner();
	}

	@Override
	public LocalNodeEndpointUpdater createLocalNodeEndpointUpdater() {
		return new LocalNodeEndpointUpdater(this.state.getLocalNode(), this.peerConnector);
	}

	@Override
	public NodeBroadcaster createNodeBroadcaster() {
		return new NodeBroadcaster(this.peerConnector);
	}

	@Override
	public NodeRefresher createNodeRefresher() {
		return new NodeRefresher(this.state.getLocalNode(), this.state.getNodes(), this.peerConnector, this.compatibilityChecker);
	}

	@Override
	public NodeSynchronizer createNodeSynchronizer() {
		return new NodeSynchronizer(this.syncConnectorPool, this.blockSynchronizer, this.state);
	}

	@Override
	public ChainServices getChainServices() {
		return this.chainServices;
	}

	@Override
	public TimeSynchronizer createTimeSynchronizer(final NodeSelector selector, final TimeProvider timeProvider) {
		return new NisTimeSynchronizer(selector, this.timeSyncStrategy, this.timeSynchronizationConnector, timeProvider, this.state);
	}

	@Override
	public NodeExperiencesUpdater createNodeExperiencesUpdater(TimeProvider timeProvider) {
		return new NodeExperiencesUpdater(this.peerConnector, timeProvider, this.state);
	}
}
