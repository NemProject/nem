package org.nem.peer.services;

import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.trust.*;

/**
 * Factory class for creating peer network services.
 */
public class PeerNetworkServicesFactory {

	private final PeerNetworkState state;
	private final PeerConnector peerConnector;
	private final SyncConnectorPool syncConnectorPool;
	private final BlockSynchronizer blockSynchronizer;

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
			final SyncConnectorPool syncConnectorPool,
			final BlockSynchronizer blockSynchronizer) {
		this.state = state;
		this.peerConnector = peerConnector;
		this.syncConnectorPool = syncConnectorPool;
		this.blockSynchronizer = blockSynchronizer;
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
		return new NodeRefresher(this.state.getLocalNode(), this.state.getNodes(), this.peerConnector);
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
	 * Creates a node selector.
	 *
	 * @return A node synchronizer.
	 */
	public NodeSelector createNodeSelector() {
		// TODO: fix me!
//		this.selector = new BasicNodeSelector(
//				new ActiveNodeTrustProvider(this.config.getTrustProvider(), this.nodes),
//				context);
		return null;
////		return new NodeSynchronizer(this.syncConnectorPool, this.blockSynchronizer, this.state);
	}
}
